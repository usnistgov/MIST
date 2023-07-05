import os
import multiprocessing
import logging



def is_ide_debug_mode():
    # check if IDE is in debug mode, and set num parallel worker to 0
    import sys
    gettrace = getattr(sys, 'gettrace', None)
    if gettrace():
        logging.info("Detected IDE debug mode")
        return True
    return False


def get_num_workers():
    # default to all the cores
    num_workers = multiprocessing.cpu_count()
    try:
        # if slurm is found use the cpu count it specifies
        num_workers = int(os.environ['SLURM_CPUS_PER_TASK'])
    except KeyError as e:
        pass  # do nothing

    logging.info("Using {} Workers".format(num_workers))
    return num_workers

