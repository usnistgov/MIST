import os
import multiprocessing
import logging



def is_ide_debug_mode():
    # check if IDE is in debug mode, and set num parallel worker to 0
    import sys
    gettrace = getattr(sys, 'gettrace', None)
    if gettrace():
        logging.info("Detected IDE debug mode, setting number of workers to 0 to allow IDE debugger to work with pytorch.")
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

    if is_ide_debug_mode():
        # IDE is debug (works at least of PyCharm), so set num workers to 0
        num_workers = 0
    logging.info("Using {} Workers".format(num_workers))
    return num_workers

