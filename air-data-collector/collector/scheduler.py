"""
数据采集定时调度模块（优化版）
使用APScheduler实现更专业的任务调度
"""

import signal
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from apscheduler.triggers.interval import IntervalTrigger
from apscheduler.events import EVENT_JOB_EXECUTED, EVENT_JOB_ERROR, JobExecutionEvent
from loguru import logger

from config.settings import settings, LOG_DIR
from collector.air_station_crawler import AirQualityCollector
from collector.weather_crawler import WeatherCollector


class DataCollectionScheduler:
    """数据采集调度器（优化版）"""

    def __init__(self):
        self.scheduler = BackgroundScheduler()
        self.is_running = False
        self.jobs: Dict[str, Any] = {}

        # 配置日志
        self._setup_logging()

        # 初始化采集器
        self.air_collector = AirQualityCollector()
        self.weather_collector = WeatherCollector()

        # 注册信号处理
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        # 注册事件监听
        self.scheduler.add_listener(
            self._job_listener,
            EVENT_JOB_EXECUTED | EVENT_JOB_ERROR
        )

        logger.info("数据采集调度器初始化完成")

    def _setup_logging(self):
        """配置日志"""
        log_file = LOG_DIR / f"scheduler_{datetime.now().strftime('%Y%m%d')}.log"

        # 添加文件日志
        logger.add(
            log_file,
            rotation="00:00",      # 每天午夜轮转
            retention="30 days",   # 保留30天
            compression="zip",     # 压缩旧日志
            encoding="utf-8",
            level="INFO",
            format="{time:YYYY-MM-DD HH:mm:ss} | {level} | {name}:{function}:{line} | {message}"
        )

    def _signal_handler(self, signum, frame):
        """处理终止信号"""
        sig_name = 'SIGINT' if signum == signal.SIGINT else 'SIGTERM'
        logger.info(f"收到{sig_name}信号，正在停止调度器...")
        self.stop()
        sys.exit(0)

    def _job_listener(self, event: JobExecutionEvent):
        """任务执行监听器"""
        if event.exception:
            logger.error(f"任务执行失败: {event.job_id}, 异常: {event.exception}")
        else:
            logger.info(f"任务执行成功: {event.job_id}")

    def job_collect_air_quality(self):
        """空气质量采集任务"""
        logger.info("========== 开始空气质量数据采集 ==========")
        try:
            with AirQualityCollector() as collector:
                report = collector.run()
                logger.info(f"空气质量采集完成: {report['statistics']['records_collected']} 条记录")
        except Exception as e:
            logger.exception(f"空气质量采集失败: {e}")

    def job_collect_weather(self):
        """气象数据采集任务"""
        logger.info("========== 开始气象数据采集 ==========")
        try:
            with WeatherCollector() as collector:
                report = collector.run()
                logger.info(f"气象数据采集完成: {report['statistics']['records_collected']} 条记录")
        except Exception as e:
            logger.exception(f"气象数据采集失败: {e}")

    def job_daily_report(self):
        """每日报告生成任务"""
        logger.info("========== 生成每日数据报告 ==========")
        try:
            timestamp = datetime.now().strftime('%Y-%m-%d')
            # 可以添加数据分析和报告生成逻辑
            logger.info(f"日期 {timestamp} 的每日报告已生成")
        except Exception as e:
            logger.exception(f"每日报告生成失败: {e}")

    def job_weekly_cleanup(self):
        """每周数据清理任务"""
        logger.info("========== 执行数据清理 ==========")
        try:
            self._cleanup_old_data()
            logger.info("过期数据清理完成")
        except Exception as e:
            logger.exception(f"数据清理失败: {e}")

    def _cleanup_old_data(self, days: int = 30):
        """清理过期数据文件"""
        import shutil
        from datetime import timedelta

        cutoff_date = datetime.now() - timedelta(days=days)
        data_dir = settings.output_dir

        cleaned = 0
        for file_path in data_dir.glob("*"):
            if file_path.is_file():
                file_time = datetime.fromtimestamp(file_path.stat().st_mtime)
                if file_time < cutoff_date:
                    file_path.unlink()
                    cleaned += 1

        logger.info(f"已清理 {cleaned} 个过期文件")

    def setup_schedule(self):
        """设置调度任务"""
        # 空气质量采集 - 每小时执行
        self.scheduler.add_job(
            self.job_collect_air_quality,
            trigger=IntervalTrigger(hours=1),
            id='air_quality_hourly',
            name='空气质量小时采集',
            replace_existing=True
        )

        # 气象数据采集 - 每3小时执行
        self.scheduler.add_job(
            self.job_collect_weather,
            trigger=IntervalTrigger(hours=3),
            id='weather_3hourly',
            name='气象数据3小时采集',
            replace_existing=True
        )

        # 每日报告 - 每天早上8点
        self.scheduler.add_job(
            self.job_daily_report,
            trigger=CronTrigger(hour=8, minute=0),
            id='daily_report',
            name='每日数据报告',
            replace_existing=True
        )

        # 每周清理 - 每周一凌晨2点
        self.scheduler.add_job(
            self.job_weekly_cleanup,
            trigger=CronTrigger(day_of_week='mon', hour=2, minute=0),
            id='weekly_cleanup',
            name='每周数据清理',
            replace_existing=True
        )

        logger.info("调度任务设置完成")
        self._print_schedule()

    def _print_schedule(self):
        """打印任务列表"""
        print("\n" + "=" * 60)
        print("已配置的调度任务")
        print("=" * 60)
        for job in self.scheduler.get_jobs():
            print(f"• {job.name} (ID: {job.id})")
            print(f"  触发器: {job.trigger}")
        print("=" * 60 + "\n")

    def start(self):
        """启动调度器"""
        self.setup_schedule()
        self.scheduler.start()
        self.is_running = True

        logger.info("数据采集调度器已启动")
        print("\n" + "=" * 60)
        print("调度器运行中...")
        print("按 Ctrl+C 停止")
        print("=" * 60 + "\n")

        try:
            while self.is_running:
                import time
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        """停止调度器"""
        if self.scheduler.running:
            self.scheduler.shutdown(wait=True)
            logger.info("调度器已停止")
        self.is_running = False

    def run_once(self, job_type: str = 'all'):
        """立即执行一次任务"""
        print("=" * 60)
        print(f"立即执行: {job_type}")
        print("=" * 60)

        if job_type in ['all', 'air']:
            self.job_collect_air_quality()

        if job_type in ['all', 'weather']:
            self.job_collect_weather()


def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description='数据采集调度器')
    parser.add_argument(
        '--mode', '-m',
        choices=['schedule', 'once', 'air', 'weather'],
        default='schedule',
        help='运行模式: schedule=定时调度, once=立即执行全部, air/weather=立即执行指定类型'
    )

    args = parser.parse_args()

    scheduler = DataCollectionScheduler()

    if args.mode == 'schedule':
        scheduler.start()
    elif args.mode == 'once':
        scheduler.run_once('all')
    elif args.mode == 'air':
        scheduler.run_once('air')
    elif args.mode == 'weather':
        scheduler.run_once('weather')


if __name__ == '__main__':
    main()
