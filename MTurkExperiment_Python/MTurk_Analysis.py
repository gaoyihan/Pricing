from matplotlib import pyplot
from collections import Counter
import numpy

fixed_price_file = ['log_group_10_002_full.txt', 
                    'log_group_20_002_full.txt', 
                    'log_group_30_002.txt', 
                    'log_group_40_002.txt',
                    'log_group_50_002.txt']

dynamic_price_file = ['log_group_dyn_002_01.txt',
                      'log_group_dyn_002_02.txt',
                      'log_group_dyn_002_03.txt',
                      'log_group_dyn_002_04.txt',
                      'log_group_dyn_002_05.txt']

quality_file = ['log_group_fix_002_1.txt',
                'log_group_fix_002_2.txt',
                'log_group_fix_002_3.txt',
                'log_group_fix_002_4.txt', 
                'log_group_fix_002_5.txt']

fixed_price_start_time = [1401289200000,
                          1401202800000,
                          1400684400000,
                          1400598000000,
                          1400511600000]

dynamic_price_start_time = [1402066800000,
                            1402326000000,
                            1402412400000,
                            1402498800000,
                            1402585200000]

style = ['-', '--', '-.', ':', '-']
marked_style = ['o-', 's--', '*-.', '^:', 'v-']


class FixedPriceArrivalLog(object):
    def __init__(self):
        self._hit_id = ''
        self._worker_id = ''
        self._accept_time = 0
        self._submit_time = 0
        
class DynamicPriceArrivalLog(object):
    def __init__(self):
        self._worker_id = ''
        self._size = 0
        self._accept_time = 0
        self._submit_time = 0
        self._right_answer = 0

def parse_fixed_price_file():
    ret = [[] for _ in range(0, 5)]
    for i in xrange(0, 5):
        infile = open(fixed_price_file[i], 'r');
        while (True):
            hit_id = infile.readline()
            worker_id = infile.readline()
            accept_time = infile.readline()
            submit_time = infile.readline()
            if submit_time == '':
                break
            log = FixedPriceArrivalLog()
            log._hit_id = hit_id
            log._worker_id = worker_id
            log._accept_time = int(accept_time) - fixed_price_start_time[i]
            log._submit_time = int(submit_time) - fixed_price_start_time[i]
            ret[i].append(log)    
    return ret

def parse_dynamic_price_file():
    ret = [[] for _ in range(0, 5)]
    for i in xrange(0, 5):
        infile = open(dynamic_price_file[i], 'r');
        while (True):
            worker_id = infile.readline()
            accept_time = infile.readline()
            size = infile.readline()
            right_answer = infile.readline()
            submit_time = infile.readline()
            if submit_time == '':
                break
            log = DynamicPriceArrivalLog()
            log._worker_id = worker_id
            log._accept_time = int(accept_time) - dynamic_price_start_time[i]
            log._submit_time = int(submit_time) - dynamic_price_start_time[i]
            log._size = int(size)
            log._right_answer = int(right_answer)
            
            ret[i].append(log)    
    return ret

def parse_quality_file():
    ret = [[] for _ in range(0, 5)]
    for i in xrange(0, 5):
        infile = open(quality_file[i], 'r');
        while (True):
            worker_id = infile.readline()
            accept_time = infile.readline()
            size = infile.readline()
            right_answer = infile.readline()
            submit_time = infile.readline()
            if submit_time == '':
                break
            log = DynamicPriceArrivalLog()
            log._worker_id = worker_id
            log._size = int(size)
            log._right_answer = int(right_answer)
            
            ret[i].append(log)    
    return ret

def figure_completion_time(fixed_log, save_figure):
    pyplot.figure(figsize=(4.5,3))
    figures = []
    for i in xrange(0, 5):
        time_array = map(lambda log:log._accept_time, fixed_log[i])
        time_array.sort()
        step = 100.0 / (500.0 / (i + 1))
        plot_x = map(lambda x:max(x / (60 * 60 * 1000.0),0), time_array) 
        plot_y = map(lambda x:x*step, range(1, len(time_array) + 1))
        plot_x.append(14)
        plot_y.append(plot_y[-1])
        figures.append(pyplot.plot(plot_x, plot_y, style[i]))
    pyplot.legend(
        (figures[0][0], figures[1][0], figures[2][0], figures[3][0], figures[4][0]), 
        ('fixed-10', 'fixed-20', 'fixed-30', 'fixed-40', 'fixed-50'),
        loc = 4, prop={'size':12}
    )
    pyplot.xlabel('Elapsed Time(h)', fontsize='large')
    pyplot.xticks(numpy.arange(0, 16, 2), fontsize='large')    
    pyplot.ylabel('Work completed(%)', fontsize='large')
    pyplot.yticks(numpy.arange(0, 120, 20), fontsize='large')        
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_completion_time.png')
    else:
        pyplot.show()

def figure_hit_time(fixed_log, save_figure):
    pyplot.figure(figsize=(4.5,3))
    figures = []
    for i in xrange(0, 5):
        time_array = map(lambda log:log._accept_time, fixed_log[i])
        time_array.sort()
        step = 1
        plot_x = map(lambda x:max(x / (60 * 60 * 1000.0),0), time_array) 
        plot_y = map(lambda x:x*step, range(1, len(time_array) + 1))
        plot_x.append(14)
        plot_y.append(plot_y[-1])
        figures.append(pyplot.plot(plot_x, plot_y, style[i]))
    pyplot.legend(
        (figures[0][0], figures[1][0], figures[2][0], figures[3][0], figures[4][0]), 
        ('fixed-10', 'fixed-20', 'fixed-30', 'fixed-40', 'fixed-50'),
        loc = 1, prop={'size':12}
    )
    pyplot.xlabel('Elapsed Time(h)', fontsize='large')
    pyplot.xticks(numpy.arange(0, 16, 2), fontsize='large')    
    pyplot.ylabel('Num of task completed', fontsize='large')
    pyplot.yticks(numpy.arange(0, 600, 100), fontsize='large')    
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_hit_time.png')
    else:
        pyplot.show()
        
def figure_dynamic_completion_time(dynamic_log, save_figure):
    pyplot.figure(figsize=(4.5,3.0))
    figures = []
    for i in xrange(0, 5):
        #print len(dynamic_log[i])
        dynamic_log[i].sort(key = lambda log:log._accept_time)
        time_array = map(lambda log:log._accept_time, dynamic_log[i])
        completion = map(lambda log:log._size / 5000.0, dynamic_log[i])
        for j in xrange(1, len(completion)):
            completion[j] += completion[j - 1]
        plot_x = map(lambda x:max(x / (60 * 60 * 1000.0),0), time_array) 
        plot_y = map(lambda x:min(x * 100.0, 100), completion)
        plot_x.append(14)
        plot_y.append(plot_y[-1])
        figures.append(pyplot.plot(plot_x, plot_y, style[i]))
    pyplot.xlabel('Elapsed Time(h)', fontsize='large')
    pyplot.xticks(numpy.arange(0, 16, 2), fontsize='large')    
    pyplot.ylabel('Work completed(%)', fontsize='large')
    pyplot.yticks(numpy.arange(0, 120, 20), fontsize='large')        
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_dynamic_completion_time.png')
    else:
        pyplot.show()
        
def figure_quality_size(dynamic_log, save_figure):
    pyplot.figure(figsize=(4.5,3.0))
    figures = []
    count20 = [0] * 21
    count50 = [0] * 51
    for i in xrange(0, 5):
        log20 = filter(lambda log:log._size == 20, dynamic_log[i])
        log50 = filter(lambda log:log._size == 50, dynamic_log[i])
        for log in log20:
            count20[log._right_answer] += 1
        for log in log50:
            count50[log._right_answer] += 1
        # Trial Accuracy
        print 'Trial', i + 1
        print sum(map(lambda log:log._right_answer / 20.0, log20)) / len(log20)
        print sum(map(lambda log:log._right_answer / 50.0, log50)) / len(log50)
        total_sum = sum(map(lambda log:log._right_answer, log20)) + sum(map(lambda log:log._right_answer, log50))
        print (total_sum * 1.0) / (len(log20) * 20 + len(log50) * 50)
        
    # Overall Accuracy
    # print sum(map(lambda i:i * count20[i] * 1.0, range(0, 21))) / sum(count20) / 20.0
    # print sum(map(lambda i:i * count50[i] * 1.0, range(0, 51))) / sum(count50) / 50.0
    plot_x = map(lambda x:x / 20.0 * 100.0, range(0, 21))
    # plot_y = map(lambda x:x * 21.0 / sum(count20), count20)
    plot_y = map(lambda x: x * 1.0 / sum(count20), count20)
    plot_y = numpy.cumsum(plot_y)
    figures.append(pyplot.plot(plot_x, plot_y, style[0]))
    plot_x = map(lambda x:x / 50.0 * 100.0, range(0, 51))
    # plot_y = map(lambda x:x * 51.0 / sum(count50), count50)
    plot_y = map(lambda x: x * 1.0 / sum(count50), count50)
    plot_y = numpy.cumsum(plot_y)
    figures.append(pyplot.plot(plot_x, plot_y, style[1]))
    pyplot.legend(
        (figures[0][0], figures[1][0]), 
        ('group-20', 'group-50'),
        loc = 2, prop={'size':12}
    )
    pyplot.xlabel('Answer Accuracy(%)', fontsize='large')
    pyplot.xticks(numpy.arange(0, 110, 10), fontsize='large')    
    # pyplot.ylabel('Probability Density', fontsize='large')
    # pyplot.yticks(numpy.arange(0, 14, 2), fontsize='large')        
    pyplot.ylabel('Cumulative Distribution', fontsize='large')
    pyplot.yticks(numpy.arange(0, 1.1, 0.1), fontsize='large')        
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_quality_size.png')
    else:
        pyplot.show()
        
def figure_static_quality_size(quality_log, save_figure):
    pyplot.figure(figsize=(4.5,3.0))
    figures = []
    count = [[0] * ((i + 1) * 10 + 1) for i in range(0, 5)]
    for log_array in quality_log:
        log = [[] for _ in range(0, 5)]
        for size in xrange(0, 5):
            log[size] = filter(lambda log:log._size == (size + 1) * 10, log_array)
        for size in xrange(0, 5):
            for log_item in log[size]:
                count[size][log_item._right_answer] += 1
    for size in xrange(0, 5):
        print sum(map(lambda i:i * count[size][i] * 1.0, range(0, size * 10 + 11))) / sum(count[size]) / (size * 10 + 10.0)
    #print sum(map(lambda i:i * count[4][i] * 1.0, range(0, 51))) / sum(count[4]) / 50.0
    for size in xrange(0, 5):
        plot_x = map(lambda x:x / (size * 10 + 10.0) * 100.0, range(0, size * 10 + 11))
        # plot_y = map(lambda x:x * (size * 10 + 11.0) / sum(count[size]), count[size])
        plot_y = map(lambda x:x * 1.0 / sum(count[size]), count[size])
        plot_y = numpy.cumsum(plot_y)
        figures.append(pyplot.plot(plot_x, plot_y, style[size]))
    pyplot.legend(
        (figures[0][0], figures[1][0], figures[2][0], figures[3][0], figures[4][0]), 
        ('group-10', 'group-20', 'group-30', 'group-40', 'group-50'),
        loc = 2, prop={'size':12}
    )
    pyplot.xlabel('Answer Accuracy(%)', fontsize='large')
    pyplot.xticks(numpy.arange(0, 110, 10), fontsize='large')    
    # pyplot.ylabel('Probability Density', fontsize='large')
    # pyplot.yticks(numpy.arange(0, 14, 2), fontsize='large')
    pyplot.ylabel('Cumulative Distribution', fontsize='large')
    pyplot.yticks(numpy.arange(0, 1.1, 0.1), fontsize='large')        
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_static_quality_size.png')
    else:
        pyplot.show()        
    
def figure_worker_task(fixed_log, save_figure):
    pyplot.figure(figsize=(4.5,3))
    average = []
    pos_x = []
    for i in xrange(0, 5):
        worker_id = map(lambda log:log._worker_id, fixed_log[i])
        count_worker = Counter(worker_id)
        task_num = count_worker.values()
        average.append(sum(task_num) * 1.0 / len(task_num))
        pos_x.append(i)
    pyplot.bar(pos_x, average, 0.6)
    pyplot.xlabel('Num of Questions per Task', fontsize='large')
    pyplot.xticks(map(lambda x:x + 0.3, pos_x), numpy.arange(10, 60, 10), fontsize='large')    
    pyplot.xlim(0, 4.6)
    pyplot.ylabel('Num of task per Worker', fontsize='large')
    #pyplot.yticks(numpy.arange(0, 600, 100), fontsize='large')    
    pyplot.tight_layout(pad = 0)
    if save_figure:
        pyplot.savefig('fig_worker_task.png')
    else:
        pyplot.show()

if __name__ == '__main__':
    save_figure = True
    fixed_log = parse_fixed_price_file();
    dynamic_log = parse_dynamic_price_file();
    quality_log = parse_quality_file();
    figure_completion_time(fixed_log, save_figure)
    figure_hit_time(fixed_log, save_figure)
    figure_dynamic_completion_time(dynamic_log, save_figure)
    figure_quality_size(dynamic_log, save_figure)
    figure_static_quality_size(quality_log, save_figure)
    figure_worker_task(fixed_log, save_figure)
    