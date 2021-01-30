import logging
import sys


class Log:
    @staticmethod
    def d(msg):
        logging.debug(msg)

    @staticmethod
    def i(msg):
        logging.info(msg)

    @staticmethod
    def w(msg):
        logging.warning(msg)

    @staticmethod
    def e(msg):
        logging.error(msg)


def _setup_logging() -> None:
    """
    Adds color formatting to logs and sets log level to INFO.
    :return: None
    """
    red = '\u001b[31m'
    green = '\u001b[32m'
    yellow = '\u001b[33m'
    blue = '\u001b[34m'
    reset = '\u001b[39;49m'

    level_colors = {
        'WARNING': yellow,
        'INFO': green,
        'DEBUG': green,
        'CRITICAL': red,
        'ERROR': red
    }

    class ColorFormatter(logging.Formatter):
        def __init__(self, log_format):
            super().__init__(log_format)

        def format(self, record):
            if record.levelname in level_colors:
                record.levelname = "{color_begin}{level:>8}{color_end}".format(
                    level=record.levelname,
                    color_begin=level_colors[record.levelname],
                    color_end=reset,
                )
            return super(ColorFormatter, self).format(record)

    ch = logging.StreamHandler(sys.stdout)
    ch.setLevel(logging.INFO)
    formatter = ColorFormatter('{}%(asctime)-15s %(levelname)s{} : %(message)s'.format(blue, reset))
    ch.setFormatter(formatter)

    logging.basicConfig(level=logging.INFO)
    logging.getLogger('').handlers = []
    logging.getLogger('').addHandler(ch)


_setup_logging()
