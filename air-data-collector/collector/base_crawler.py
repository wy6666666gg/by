"""
数据采集基类 - 提供通用功能
"""

import json
import time
import hashlib
from abc import ABC, abstractmethod
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Dict, Any, Callable
from functools import wraps

import pandas as pd
import requests
from loguru import logger
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from config.settings import settings, LOG_DIR


@dataclass
class CrawlerStats:
    """采集统计信息"""
    start_time: datetime
    end_time: Optional[datetime] = None
    total_requests: int = 0
    success_requests: int = 0
    failed_requests: int = 0
    records_collected: int = 0
    errors: List[str] = None

    def __post_init__(self):
        if self.errors is None:
            self.errors = []

    @property
    def duration_seconds(self) -> float:
        end = self.end_time or datetime.now()
        return (end - self.start_time).total_seconds()

    @property
    def success_rate(self) -> float:
        if self.total_requests == 0:
            return 0.0
        return self.success_requests / self.total_requests

    def to_dict(self) -> Dict[str, Any]:
        return {
            'start_time': self.start_time.isoformat(),
            'end_time': self.end_time.isoformat() if self.end_time else None,
            'duration_seconds': self.duration_seconds,
            'total_requests': self.total_requests,
            'success_requests': self.success_requests,
            'failed_requests': self.failed_requests,
            'success_rate': f"{self.success_rate:.2%}",
            'records_collected': self.records_collected,
            'errors': self.errors
        }


def rate_limited(max_per_second: float = 2.0):
    """请求频率限制装饰器"""
    min_interval = 1.0 / max_per_second
    last_call_time = [0.0]

    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            elapsed = time.time() - last_call_time[0]
            if elapsed < min_interval:
                time.sleep(min_interval - elapsed)
            last_call_time[0] = time.time()
            return func(*args, **kwargs)
        return wrapper
    return decorator


class BaseCrawler(ABC):
    """数据采集基类"""

    def __init__(self, output_dir: Optional[Path] = None, name: str = "crawler"):
        self.name = name
        self.output_dir = output_dir or settings.output_dir
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.session = requests.Session()
        self.stats = CrawlerStats(start_time=datetime.now())
        self._setup_session()

    def _setup_session(self):
        """配置HTTP会话"""
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
                         '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'application/json, text/plain, */*',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Cache-Control': 'no-cache',
        })
        # 配置连接池
        adapter = requests.adapters.HTTPAdapter(
            pool_connections=10,
            pool_maxsize=20,
            max_retries=3
        )
        self.session.mount('http://', adapter)
        self.session.mount('https://', adapter)

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        retry=retry_if_exception_type((requests.RequestException, ConnectionError)),
        reraise=True
    )
    def _make_request(self, url: str, method: str = 'GET',
                      **kwargs) -> requests.Response:
        """发送HTTP请求（带重试机制）"""
        self.stats.total_requests += 1
        try:
            response = self.session.request(
                method=method,
                url=url,
                timeout=settings.api_timeout,
                **kwargs
            )
            response.raise_for_status()
            self.stats.success_requests += 1
            return response
        except Exception as e:
            self.stats.failed_requests += 1
            error_msg = f"请求失败 {url}: {str(e)}"
            self.stats.errors.append(error_msg)
            logger.error(error_msg)
            raise

    @rate_limited(max_per_second=2.0)
    def fetch_data(self, url: str, **kwargs) -> Optional[Dict[str, Any]]:
        """获取JSON数据"""
        try:
            response = self._make_request(url, **kwargs)
            return response.json()
        except Exception as e:
            logger.error(f"数据解析失败: {e}")
            return None

    def save_data(self, data: List[Dict], filename_prefix: str = None,
                  formats: List[str] = None) -> Dict[str, Path]:
        """保存数据到文件"""
        if not data:
            logger.warning("没有数据需要保存")
            return {}

        if formats is None:
            formats = ['json', 'csv'] if settings.output_format == 'both' else [settings.output_format]

        timestamp = datetime.now()
        date_str = timestamp.strftime('%Y%m%d')
        time_str = timestamp.strftime('%H%M%S')
        prefix = filename_prefix or self.name

        saved_files = {}

        for fmt in formats:
            if fmt == 'json':
                filepath = self.output_dir / f"{prefix}_{date_str}_{time_str}.json"
                with open(filepath, 'w', encoding='utf-8') as f:
                    json.dump({
                        'metadata': {
                            'collection_time': timestamp.isoformat(),
                            'record_count': len(data),
                            'crawler_name': self.name
                        },
                        'data': data
                    }, f, ensure_ascii=False, indent=2, default=str)
                saved_files['json'] = filepath
                logger.info(f"JSON数据已保存: {filepath}")

            elif fmt == 'csv':
                filepath = self.output_dir / f"{prefix}_{date_str}_{time_str}.csv"
                df = pd.DataFrame(data)
                df.to_csv(filepath, index=False, encoding='utf-8-sig')
                saved_files['csv'] = filepath
                logger.info(f"CSV数据已保存: {filepath}")

        return saved_files

    def generate_report(self) -> Dict[str, Any]:
        """生成采集报告"""
        self.stats.end_time = datetime.now()
        report = {
            'crawler_name': self.name,
            'statistics': self.stats.to_dict(),
            'saved_files': getattr(self, '_last_saved_files', {})
        }

        # 保存报告
        report_path = self.output_dir / f"report_{self.name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)

        logger.info(f"采集报告已生成: {report_path}")
        return report

    @abstractmethod
    def collect(self) -> List[Dict[str, Any]]:
        """执行数据采集（子类必须实现）"""
        pass

    def run(self) -> Dict[str, Any]:
        """运行采集流程"""
        logger.info(f"【{self.name}】开始数据采集...")
        try:
            data = self.collect()
            self.stats.records_collected = len(data)

            if data:
                self._last_saved_files = self.save_data(data)

            report = self.generate_report()
            logger.info(f"【{self.name}】采集完成: 成功 {self.stats.success_requests}, "
                       f"失败 {self.stats.failed_requests}, 数据量 {len(data)}")
            return report

        except Exception as e:
            logger.exception(f"【{self.name}】采集过程发生错误: {e}")
            self.stats.errors.append(str(e))
            raise

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.session.close()
