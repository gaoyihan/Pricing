import numpy
from numpy import random
from scipy.stats import norm
from matplotlib import pyplot
import time
import urllib
import math

def discrete_choice():
    random.seed()
    beta = 2.6
    cnt = [0] * 100
    est = [0] * 100
    for ex in range(0, 100):
        print "experiment:", ex
        mu = []; sigma = []
        for i in range(0, 100):
            mu.append(random.normal())
            sigma.append(random.uniform() * 1)
        sumExpU = sum([math.exp(beta * x) for x in mu])
        for times in range(0, 100):
            U = []
            for i in range(0, 100):
                U.append(random.normal(loc = mu[i], scale = sigma[i]))
            maxU = max(U)
            for c in range(0, 100):
                mu2 = c / 50.0 - 1
                sigma2 = random.uniform()
                cnt[c] += 1 - norm.cdf((maxU - mu2) / sigma2)
                est[c] += math.exp(beta * mu2) / (sumExpU + math.exp(beta * mu2))
    for c in range(0, 100):
        cnt[c] /= 10000
        est[c] /= 10000
    pyplot.figure(figsize=(5,3))
    pyplot.xlabel('Task Price', fontsize='large')
    pyplot.ylabel('Task Acceptance Prob.', fontsize='large')
    pyplot.xticks(range(0, 110, 10), fontsize='large')
    pyplot.xlim(0, 100)
    pyplot.yticks(numpy.arange(0.0, 0.009, 0.001), fontsize='large')
    pyplot.ylim(0, 0.008)
    pyplot.plot(range(0, 100), cnt, 'bo', range(0, 100), est, 'r-')
    pyplot.tight_layout(pad=0)
    pyplot.show()
    for c in range(0, 100):
        print c, cnt[c]
        
def finds0(lmbda):
    for s in range(lmbda + 1, lmbda + 100):
        if (math.exp(-lmbda) * lmbda**s / math.factorial(s) * s / (s - lmbda) < 10**(-9)):
            return s
    return -1

def calc_acc_prob(c, biasc, scalec, totalc):
    return math.exp(c / scalec - biasc) / (totalc + math.exp(c / scalec - biasc))

def calc_pois_prob(i, lmbda):
    res = math.exp(-lmbda)
    for j in range(1, i + 1):
        res *= lmbda / j
        if res < 1e-16 and j > lmbda:
            return 0
    return res
       
def simulateDP(price, lmbda, N, NT, alpha, biasc, scalec, totalc):
    cost = []
    ave_cost = []
    remain = []
    for exp in range(0, 100000):
        remain_subtasks = N
        total_cost = 0
        for i in range(0, NT):
            completion = random.poisson(lam = lmbda[i] * calc_acc_prob(price[remain_subtasks][i], biasc, scalec, totalc))
            if completion > remain_subtasks:
                completion = remain_subtasks
            total_cost += completion * price[remain_subtasks][i]
            remain_subtasks -= completion
        cost.append(total_cost)
        ave_cost.append(total_cost * 1.0 / (N - remain_subtasks))
        penalty = remain_subtasks
        if remain_subtasks > 0:
            penalty += alpha
        remain.append(penalty)
    mean_cost = sum(map(lambda x:x * 1.0, cost)) * 1.0 / len(cost)
    mean_remain = sum(map(lambda x:x * 1.0, remain)) * 1.0 / len(remain)
    mean_ave_cost = sum(map(lambda x:x * 1.0, ave_cost)) * 1.0 / len(ave_cost)    
    print mean_cost, mean_ave_cost, mean_remain
    return mean_cost, mean_ave_cost, mean_remain

def simulatefixed(price, lmbda, N, NT, alpha, biasc, scalec, totalc):
    cost = []
    ave_cost = []
    remain = []
    for exp in range(0, 10000):
        remain_subtasks = N
        total_cost = 0
        for i in range(0, NT):
            completion = random.poisson(lam = lmbda[i] * calc_acc_prob(price, biasc, scalec, totalc))
            if completion > remain_subtasks:
                completion = remain_subtasks            
            total_cost += completion * price
            remain_subtasks -= completion
        cost.append(total_cost)
        ave_cost.append(total_cost * 1.0 / (N - remain_subtasks))
        penalty = remain_subtasks
        if remain_subtasks > 0:
            penalty += alpha
        remain.append(penalty)
    mean_cost = sum(map(lambda x:x * 1.0, cost)) * 1.0 / len(cost)
    mean_remain = sum(map(lambda x:x * 1.0, remain)) * 1.0 / len(remain)
    mean_ave_cost = sum(map(lambda x:x * 1.0, ave_cost)) * 1.0 / len(ave_cost)    
    print mean_cost, mean_ave_cost, mean_remain
    return mean_cost, mean_ave_cost, mean_remain
    
def calc_state_cost(opt, price, lmbda, n, t, L, U, biasc, scalec, totalc):
    opt[n][t] = 1e10
    for c in range(L, U + 1):
        cost = 0; pr = 0
        acceptrate = calc_acc_prob(c, biasc, scalec, totalc)
        for i in range(0, n):
            p = calc_pois_prob(i, lmbda[t] * acceptrate)
            cost = cost + p * (i * c + opt[n - i][t + 1])
            pr = pr + p
            if 1 - pr < 1e-14:
                break
        cost = cost + (1 - pr) * n * c
        if cost < opt[n][t]:
            opt[n][t] = cost
            price[n][t] = c    

def calc_state_policy_for_time(opt, price, lmbda, t, l, r, L, U, biasc, scalec, totalc):
    m = (l + r) / 2
    calc_state_cost(opt, price, lmbda, m, t, L, U, biasc, scalec, totalc)
    pm = price[m][t]
    if l < m:
        calc_state_policy_for_time(opt, price, lmbda, t, l, m - 1, L, pm, biasc, scalec, totalc)
    if m < r:
        calc_state_policy_for_time(opt, price, lmbda, t, m + 1, r, pm, U, biasc, scalec, totalc)

def calc_state_policy(opt, price, N, NT, lmbda, maxC, Penalty, alpha, biasc, scalec, totalc):
    #Initialization
    for i in range(0, N + 1):
        opt[i][NT] = (i + alpha) * Penalty
    opt[0][NT] = 0
    #Dynamic Programming
    for t in range(NT - 1, -1, -1):
        print 'DP for time slot', t
        for i in range(0, N + 1):
            calc_state_cost(opt, price, lmbda, i, t, 1, maxC, biasc, scalec, totalc)
    #Write Result
    logfile = open('state2.log', 'w')
    for i in range(0, N + 1):
        for t in range(0, NT + 1):
            logfile.write('{} {} {} {}\n'.format(i, t, opt[i][t], price[i][t]))
    logfile.close()
    
def improved_calc_state_policy(opt, price, N, NT, lmbda, maxC, Penalty, alpha, biasc, scalec, totalc):
    #Initialization
    for i in range(0, N + 1):
        opt[i][NT] = (i + alpha) * Penalty
    opt[0][NT] = 0
    #Dynamic Programming
    start = time.clock()
    for t in range(NT - 1, -1, -1):
        calc_state_policy_for_time(opt, price, lmbda, t, 0, N, 1, maxC, biasc, scalec, totalc)
    end = time.clock()
    print 'Time elapsed:', end - start
    #Write Result
    logfile = open('state2.log', 'w')
    for i in range(0, N + 1):
        for t in range(0, NT + 1):
            logfile.write('{} {} {} {}\n'.format(i, t, opt[i][t], price[i][t]))
    logfile.close()

def calc_remain(remain, price, N, NT, lmbda, biasc, scalec, totalc):
    #Initialization
    for i in range(0, N + 1):
        remain[i][NT] = i;
    #Dynamic Programming
    for t in range(NT - 1, -1, -1):
        for n in range(0, N + 1):
            rem = 0; pr = 0; c = price[n][t]
            acceptrate = calc_acc_prob(c, biasc, scalec, totalc)
            for i in range(0, n):
                p = calc_pois_prob(i, lmbda[t] * acceptrate)
                rem = rem + p * remain[n - i][t + 1]
                pr = pr + p
                if 1 - pr < 1e-14:
                    break
            remain[n][t] = rem
        
def read_state_policy(opt, price):
    infile = open('state2.log', 'r')
    for line in infile:
        a, b, c, d = line.split()
        opt[int(a)][int(b)] = float(c)
        price[int(a)][int(b)] = int(d)
    
def fixed_deadline_simulation():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    arrival = []
    for line in infile:
        arrival.append(int(line))
    #Simulation settings
    Gran = 1
    NT = 24 * 7 * 3 / Gran
    N = 500
    Penalty = 8000
    alpha = 0
    biasc = 1.0
    scalec = 25.0
    totalc = 2000
    lmbda = []
    start_train = 0
    for i in range(start_train, start_train + NT):
        lmbda.append(sum(arrival[i * Gran:(i + 1) * Gran]))
    for i in range(NT - 12, NT):
        lmbda[i] /= 2
        
    #Simple DP
    opt = [[0 for x in xrange(0, NT + 1)] for x in xrange(0, N + 1)]
    price = [[0 for x in xrange(0, NT + 1)] for x in xrange(0, N + 1)]
    #use simple DP
    #calc_state_policy(opt, price, N, NT, lmbda, Penalty, alpha, biasc, scalec, totalc)
    #or use improved DP
    improved_calc_state_policy(opt, price, N, NT, lmbda, 100, Penalty, alpha, biasc, scalec, totalc)
    #or alternatively read state policy from file since calculation is slow
    #read_state_policy(opt, price)
    
    #Using history data to train and future data to test
    start_test = NT
    test_lmbda = []
    for i in range(start_test, start_test + NT):
        test_lmbda.append(sum(arrival[i * Gran:(i + 1) * Gran]))

    print sum(lmbda), sum(test_lmbda)
    print find_lower_bound(N, sum(test_lmbda), biasc, scalec, totalc)
            
    #Simulation DP
    print 'Start Simulation'
    simulateDP(price, test_lmbda, N, NT, alpha, biasc, scalec, totalc)

    #total = sum(lmbda)
    #Simulation fixed
    #for p in range(23, 28):
    #    print 'Static Price', p
    #    simulatefixed(p, test_lmbda, N, NT, alpha, biasc, scalec, totalc)

def fixed_price_with_bound(N, total, biasc, scalec, totalc, bound):
    l = 0; r = 100;
    while l < r:
        pr = 0
        m = (l + r) / 2
        acc = calc_acc_prob(m, biasc, scalec, totalc)
        for i in range(1, N + 1):
            pr += calc_pois_prob(N - i, total * acc)
        if pr < bound and total * acc > N:
            r = m
        else:
            l = m + 1
    return l

def fixed_deadline_simulation_with_bound():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    arrival = []
    for line in infile:
        arrival.append(int(line))
    #Simulation settings
    Gran = 1
    NT = 24 * 3 / Gran
    N = 200
    bound = 0.001
    #Use particular day to train model
    lmbda = []
    start_train = NT * 0
    for i in range(start_train, start_train + NT):
        lmbda.append(sum(arrival[i * Gran:(i + 1) * Gran]))
    #Use Average Data to Train Model
    #ave_arrival = [(sum([arrival[i*NT*7+x] for i in range(0, 4)]) - arrival[start_train + x]) / 3 
    #        for x in range(0, NT)]
    #lmbda = ave_arrival
    #Training Mapping Function
    time = 120
    biasc = math.log(time) - 5.18
    scalec = time / 8.0
    totalc = 2000
            
    #Using history data to train and future data to test
    test_lmbda = []
    start_test = NT * 0
    for i in range(start_test, start_test + NT):
        test_lmbda.append(sum(arrival[i * Gran:(i + 1) * Gran]))  
        
    print sum(lmbda), sum(test_lmbda)      
    
    #Output Other Information
    print 'Lower Bound'
    print find_lower_bound(N, sum(test_lmbda), biasc, scalec, totalc)
    print 'Fixed Price'
    fixed_price = fixed_price_with_bound(N, sum(lmbda), biasc, scalec, totalc, bound)
    print fixed_price
    
    #DP    
    opt = [[0 for _ in xrange(0, NT + 1)] for _ in xrange(0, N + 1)]
    price = [[0 for _ in xrange(0, NT + 1)] for _ in xrange(0, N + 1)]
    remain = [[0 for _ in xrange(0, NT + 1)] for _ in xrange(0, N + 1)]
    lPenalty = 20.0; rPenalty = 1e4
    while abs(remain[N][0] - bound) > 3 * 1e-4:
        print lPenalty, rPenalty
        Penalty = (lPenalty + rPenalty) / 2.0
        improved_calc_state_policy(opt, price, N, NT, lmbda, 50, Penalty, 0, biasc, scalec, totalc)
        calc_remain(remain, price, N, NT, lmbda, biasc, scalec, totalc)
        if remain[N][0] < bound:
            rPenalty = Penalty
        else:
            lPenalty = Penalty
        print remain[N][0]
            
    #Testing Mapping Function
    time = 120
    biasc = math.log(time) - 5.18
    scalec = time / 8.0
    totalc = 2000
            
    #Simulation DP
    print 'Start Simulation'
    _, ave, remain = simulateDP(price, test_lmbda, N, NT, 0, biasc, scalec, totalc)
    print 'Reduction'
    print 1 - ave / fixed_price

    #Simulation fixed
    for p in range(fixed_price - 5, fixed_price + 1):
        print 'Static Price', p
        simulatefixed(p, test_lmbda, N, NT, 0, biasc, scalec, totalc)
    
def prepare_data():
    infile = open('hit_complete.log', 'r')
    taskno = 0
    for line in infile:
        taskid,a,b,c,d = line.split()
        start = int(a)
        end = int(b)
        completed = int(c)
        left = int(d)
        if completed < 50:
            continue
        
        html = urllib.urlopen('http://mturk-tracker.com/api/hitgroupcontent/{}/?format=json'.format(taskid)).read()
        content = html.split()
        for i in range(0, len(content)):
            if content[i] == '"reward":':
                reward = float(content[i + 1][:-1])
            elif content[i] == '"description":':
                for j in range(i, len(content)):
                    if content[j] == '"group_id":':
                        description = content[i + 1:j - 1] + [content[j - 1][:-1]]
                        break
            elif content[i] == '"title":':
                title = content[i + 1:len(content) - 1] + [content[len(content) - 1][:-1]]
        if len(description) < 20:
            continue
        taskno += 1
        outfile = open('preprocess/{}.txt'.format(taskno), 'w')
        outfile.write('{}\n{}\n{}\n{}\n{}\n{}\n{}\n'.format(start, end, completed, left, reward, ' '.join(description), ' '.join(title)))

def find_lower_bound(N, total, biasc, scalec, totalc):
    l = 0
    r = 50.0
    while r-l>1e-6:
        mid = (l + r)/2
        if total * calc_acc_prob(mid, biasc, scalec, totalc) < N:
            l = mid
        else:
            r = mid
    return l

def sensitivity_simulation():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    arrival = []
    for line in infile:
        arrival.append(int(line))
    #Simulation settings
    NT = 24 * 7 * 3
    N = 500
    Penalty = 200
    alpha = 0
    biasc = 1.0

    #read state policy from file
    opt = [[0 for x in xrange(0, NT + 1)] for x in xrange(0, N + 1)]
    price = [[0 for x in xrange(0, NT + 1)] for x in xrange(0, N + 1)]
    read_state_policy(opt, price)

    res_dp_mc = []
    res_dp_mac = []
    res_dp_mr = []
    res_dp_oh = []
    res_fp_mc = [[] for x in xrange(0, 5)]
    res_fp_mac = [[] for x in xrange(0, 5)]
    res_fp_mr = [[] for x in xrange(0, 5)]
    res_fp_oh = [[] for x in xrange(0, 5)]
    dplogfile = open('sensitivity_dp.log', 'w')
    fplogfile = open('sensitivity_fp.log', 'w')
    for exp in range(0, 400):
        scalec = 25.0 + random.normal(loc=0, scale=2)
        totalc = 2000 + random.normal(loc=0, scale=50)
        lmbda = []
        for i in range(0, NT):
            lmbda.append(max(arrival[i] + random.normal(loc=0, scale=50), 0))

        #Simulation DP
        print 'Start Simulation', exp
        mc, mac, mr = simulateDP(price, lmbda, N, NT, alpha, biasc, scalec, totalc)
        res_dp_mc.append(mc)
        res_dp_mac.append(mac)
        res_dp_mr.append(mr)
        print 'Lower Bound'
        lb = find_lower_bound(N, sum(lmbda), biasc, scalec, totalc)
        res_dp_oh.append(mac / lb - 1)
        print lb, mac/lb - 1
        dplogfile.write('{} {} {} {}\n'.format(mc, mac, mr, lb))
        for p in range(23, 28):
            mc, mac, mr = simulatefixed(p, lmbda, N, NT, alpha, biasc, scalec, totalc)
            res_fp_mc[p - 23].append(mc)
            res_fp_mac[p - 23].append(mac)
            res_fp_mr[p - 23].append(mr)
            res_fp_oh[p - 23].append(mac / lb - 1)
            print mc, mac, mr, mac / lb - 1
            fplogfile.write('{} {} {} {} {}\n'.format(p, mc, mac, mr, lb))
    dplogfile.close()
    fplogfile.close()

    print 'Summary:'
    print sum(res_dp_mc) / len(res_dp_mc), sum(res_dp_mac) / len(res_dp_mac), sum(res_dp_mr) / len(res_dp_mr), sum(res_dp_oh) / len(res_dp_oh)
    for p in range(0, 5):
        print 'Static ', p + 23
        print sum(res_fp_mc[p]) / len(res_fp_mc[p]), sum(res_fp_mac[p]) / len(res_fp_mac[p]), sum(res_fp_mr[p]) / len(res_fp_mr[p]), sum(res_fp_oh[p]) / len(res_fp_oh[p])

def calc_cumulative_rate(prob, N):
    l = 0.1; r = 1e6
    while r - l > 1e-4:
        mid = (l + r) / 2
        cum = 0;
        for i in range(0, N + 1):
            cum += calc_pois_prob(i, mid)
        if cum > prob:
            l = mid
        else:
            r = mid
    return l

def static_simulation():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    lmbda = []
    for line in infile:
        lmbda.append(int(line))
    #Simulation settings
    N = 200
    time = 120
    biasc = math.log(time) - 5.18
    scalec = time / 8.0
    totalc = 2000
    res = []
    for exp in range(0, 10000):
        if exp % 10 == 0:
            print exp
        acc1 = calc_acc_prob(12, biasc, scalec, totalc)
        prob1 = random.random()
        cum1 = calc_cumulative_rate(prob1, N / 2)
        acc2 = calc_acc_prob(13, biasc, scalec, totalc)
        prob2 = random.random()
        cum2 = calc_cumulative_rate(prob2, N / 2)
        cum = cum1 / acc1 + cum2 / acc2
        for i in range(0, len(lmbda)):
            if cum < lmbda[i]:
                res.append(i + cum / lmbda[i])
                break
            else:
                cum -= lmbda[i]
    outfile = open('static_simulation.log', 'w')
    for i in range(0, len(res)):
        outfile.write(str(res[i]) + '\n')

def plot_static():
    infile = open('static_simulation.log', 'r')
    res = []
    for line in infile:
        res.append(float(line) / 3)
    pyplot.figure(figsize=(5,3))
    pyplot.hist(res, normed=1, bins=20)
    pyplot.xlabel('Completion Time(h)', fontsize='large')
    pyplot.ylabel('Probability Density', fontsize='large')
    pyplot.xticks(range(16, 34, 2), fontsize='large')
    pyplot.xlim(16, 32)
    pyplot.yticks(map(lambda x:x/100.0, range(0, 24, 4)), fontsize='large')
    pyplot.tight_layout(pad = 0)
    pyplot.show()        

def mean_std(a):
    mean = sum(a) / len(a)
    var = sum(map(lambda x:pow(x - mean, 2), a)) / len(a)
    return math.sqrt(var) / math.sqrt(len(a))

def plot_sensitivity():
    dplogfile = open('sensitivity_dp.log', 'r')
    dp_res = []
    for line in dplogfile:
        mc, mac, mr, lb = line.split()
        dp_res.append(float(mr))
    fplogfile = open('sensitivity_fp.log', 'r')
    fp_res = [[] for x in xrange(0, 5)]
    for line in fplogfile:
        p, mc, mac, mr, lb = line.split()
        fp_res[int(p) - 23].append(float(mr))
    width = 0.35
    acc_xpos = range(0, 6)
    nois_xpos = [x + width for x in range(0, 6)]
    acc_mr = (0.0914, 10.92918, 3.08725, 0.48722, 0.03741, 0.00141)
    nois_mr = (sum(dp_res)/len(dp_res), sum(fp_res[0])/len(fp_res[0]), sum(fp_res[1])/len(fp_res[1]), sum(fp_res[2])/len(fp_res[2]), sum(fp_res[3])/len(fp_res[3]), sum(fp_res[4])/len(fp_res[4]))
    acc_mr_std = (0.01, 0.01, 0.01, 0.01, 0.01, 0.01)
    nois_mr_std = (mean_std(dp_res), mean_std(fp_res[0]), mean_std(fp_res[1]), mean_std(fp_res[2]), mean_std(fp_res[3]), mean_std(fp_res[4]))

    p1 = pyplot.bar(acc_xpos, acc_mr, width, color='b', yerr=acc_mr_std)
    p2 = pyplot.bar(nois_xpos, nois_mr, width, color='r', yerr=nois_mr_std)
    pyplot.xticks(nois_xpos, ('DP', 'FP-23', 'FP-24', 'FP-25', 'FP-26', 'FP-27'))
    pyplot.yticks(range(0, 20))
    pyplot.ylim(0, 20)
    pyplot.legend( (p1[0], p2[0]), ('Accurate Parameter', 'Inaccurate Parameter') )
    pyplot.ylabel('Remain Subtasks')
    pyplot.show()
    
def plot_sensitivity_2():
    dplogfile = open('sensitivity_dp.log', 'r')
    dp_res = []
    for line in dplogfile:
        mc, mac, mr, lb = line.split()
        dp_res.append(float(mac) / float(lb) - 1)
    pyplot.hist(dp_res, 50)
    pyplot.xlabel('Overhead')
    pyplot.ylabel('Frequency')
    pyplot.show()

def plot_para():
    scale = [11, 13, 15, 17, 19]
    ave_scale = [9.07, 10.7, 12.36, 14.02, 15.67]
    fix_scale = [12, 14, 16, 18, 20]
    reduction_scale = [0.243, 0.234, 0.227, 0.221, 0.216]
    #lb_scale = [13.91, 18.55, 23.19, 27.83, 32.46]

    bias = [-0.79, -0.59, -0.39, -0.19, 0.01]
    ave_bias = [6.38, 9.37, 12.36, 15.37, 18.37]
    fix_bias = [10, 13, 16, 19, 22]
    reduction_bias = [0.361, 0.278, 0.227, 0.190, 0.164]
    #lb_bias = [10.69, 16.94, 23.19, 29.44, 35.69]

    total = [1600, 1800, 2000, 2200, 2400]
    ave_total = [9.02, 10.80, 12.36, 13.42, 15.10]
    fix_total = [12, 14, 16, 17, 18]
    reduction_total = [0.247, 0.228, 0.227, 0.18, 0.16]
    #lb_total = [17.61, 20.55, 23.19, 25.57, 27.75]

    pyplot.figure(figsize=(3.5,2.5))
    pyplot.xlabel('s parameter value', fontsize='large')
    pyplot.ylabel('Total Cost Reduction', fontsize='large')
    pyplot.plot(scale, reduction_scale, 'bo-')
    pyplot.xticks(range(11, 21, 2), fontsize='large')
    pyplot.yticks(numpy.arange(0, 0.5, 0.1), fontsize='large')
    pyplot.ylim(0, 0.4)
    #l2 = pyplot.plot(scale, fix_scale, 'rs-')
    #l3 = pyplot.plot(scale, lb_scale, 'y*-')
    #pyplot.legend( (l1[0], l2[0], l3[0]), ('Dynamic Pricing', 'Fixed Pricing', 'Theoretical Lower Bound'), loc = 4 )
    pyplot.tight_layout(pad=0)
    pyplot.show()    

    pyplot.figure(figsize=(3.5,2.5))
    pyplot.xlabel('b parameter value', fontsize='large')
    pyplot.ylabel('Total Cost Reduction', fontsize='large')
    pyplot.plot(bias, reduction_bias, 'bo-')
    pyplot.ylim(0, 0.4)   
    pyplot.xlim(-0.8,0.01) 
    pyplot.xticks(numpy.arange(-0.8, 0.2, 0.2), fontsize='large')
    pyplot.yticks(numpy.arange(0, 0.5, 0.1), fontsize='large')
    #l2 = pyplot.plot(bias, fix_bias, 'rs-')
    #l3 = pyplot.plot(bias, lb_bias, 'y*-')
    #pyplot.legend( (l1[0], l2[0], l3[0]), ('Dynamic Pricing', 'Fixed Pricing', 'Theoretical Lower Bound'), loc = 4 )
    pyplot.tight_layout(pad=0)
    #pyplot.show()    

    pyplot.figure(figsize=(3.5,2.5))
    pyplot.xlabel('M parameter value', fontsize='large')
    pyplot.ylabel('Total Cost Reduction', fontsize='large')
    pyplot.plot(total, reduction_total, 'bo-')
    pyplot.ylim(0, 0.4)    
    pyplot.xticks(numpy.arange(1600, 2600, 200), fontsize='large')
    pyplot.yticks(numpy.arange(0, 0.5, 0.1), fontsize='large')
    #l2 = pyplot.plot(total, fix_total, 'rs-')
    #l3 = pyplot.plot(total, lb_total, 'y*-')
    #pyplot.legend( (l1[0], l2[0], l3[0]), ('Dynamic Pricing', 'Fixed Pricing', 'Theoretical Lower Bound'), loc = 4 )
    pyplot.tight_layout(pad=0)
    #pyplot.show()

def plot_price_remain():
    ploty = [11.653, 12.0769, 12.2751, 12.3630, 12.3833]
    plotx = [5.6951, 1.2998, 0.1486, 0.0034, 0.0002]
    ploty2 = [12, 13, 14, 15, 16]
    plotx2 = [5.6951, 1.2998, 0.1486, 0.0034, 0.0002]
    
    pyplot.figure(figsize=(5,3))
    pyplot.xlabel('expected number of remaining tasks', fontsize='large')
    pyplot.ylabel('ave. task reward(cent)', fontsize='large')
    l1 = pyplot.semilogx(plotx, ploty, 'bo-')
    l2 = pyplot.semilogx(plotx2, ploty2, 'rs--')
    pyplot.xticks( [1e-4, 1e-3, 1e-2, 1e-1, 1, 10], fontsize='large')
    pyplot.yticks( numpy.arange(11.5, 16.5, 0.5), fontsize='large' )
    pyplot.legend( (l1[0], l2[0]), ('Dynamic Pricing', 'Fixed Pricing'), prop={'size':12} )
    pyplot.tight_layout(pad=0)
    pyplot.show()

def plot_granularity():
    gran = [20, 40, 60, 80, 120]
    ave = [12.373, 12.440, 12.512, 12.586, 12.746]
    #time = [19, 13, 11, 10, 9]
    
    pyplot.figure(figsize=(5,3))
    pyplot.xlabel('Time Interval Length', fontsize='large')
    pyplot.ylabel('Average Task Reward', fontsize='large')
    pyplot.xticks(range(20, 140, 20), fontsize='large')
    pyplot.yticks(numpy.arange(12.35, 12.75, 0.1), fontsize='large')
    pyplot.ylim(12.35, 12.75)
    pyplot.plot(gran, ave, 'bo-')
    pyplot.tight_layout(pad = 0)
    pyplot.show()   

    #pyplot.xlabel('Time Interval Length')
    #pyplot.ylabel('Algorithm Running Time')
    #pyplot.plot(gran, time, 'bo-')
    #pyplot.show()
    
def plot_time_sensitivity():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    arrival = []
    for line in infile:
        arrival.append(int(line))
    
    #Plot both time arrival rate
    NT = 24 * 3
    #ave_arrival = [sum([arrival[i*NT*7+x] for i in range(1, 4)]) / 3 for x in range(0, NT)]
    ave_arrival = [sum([arrival[i*NT*7+x] for i in range(0, 3)]) / 3 for x in range(0, NT)]
    pyplot.figure(figsize=(5,3))
    pyplot.xlabel('Hour of day', fontsize='large')
    pyplot.ylabel('Arrival Rate(arrival/hour)', fontsize='large')
    l1 = pyplot.plot([x / 3.0 for x in range(0, NT)], map(lambda x:x*3, ave_arrival), 'bo-')
    #l2 = pyplot.plot([x / 3.0 for x in range(0, NT)], map(lambda x:x*3, arrival[NT*0:NT*1]), 'rs--')
    l2 = pyplot.plot([x / 3.0 for x in range(0, NT)], map(lambda x:x*3, arrival[NT*21:NT*22]), 'rs--')
    pyplot.xticks(range(0, 25, 4), fontsize='large')
    pyplot.xlim(0, 24)
    pyplot.yticks(range(0, 28000, 4000), fontsize='large')
    #pyplot.legend( (l1[0], l2[0]), ('Average on 1.8, 1.15, 1.22', 'Arrival Rate on 1.1'), prop={'size':12} )
    pyplot.legend( (l1[0], l2[0]), ('Average on 1.1, 1.8, 1.15', 'Arrival Rate on 1.22'), prop={'size':12} )
    pyplot.tight_layout(pad=0)
    #pyplot.show()

    #Plot result for different days
    date = [1, 8, 15, 22]
    ave = [14.18, 6.43, 5.25, 7.7]
    remain = [21.2, 0, 0, 0]
    fix = [9, 11, 11, 10]
    fix_remain = [36, 0, 0, 0.009]

    #Plot Reward
    pyplot.figure(figsize=(5, 3))
    pyplot.xlabel('Date', fontsize='large')
    #pyplot.ylabel('Average Task Reward', fontsize='large')
    pyplot.ylabel('Num of Remaining Tasks', fontsize='large')
    width = 0.25
    dyn_xpos = [x + width for x in range(0, 4)]
    fix_xpos = [x + width * 2 for x in range(0, 4)]
    
    #p1 = pyplot.bar(dyn_xpos, ave, width, color='b')
    #p2 = pyplot.bar(fix_xpos, fix, width, color='r')
    p1 = pyplot.bar(dyn_xpos, remain, width, color='b')
    p2 = pyplot.bar(fix_xpos, fix_remain, width, color='r')
    pyplot.xticks(fix_xpos, ('1', '8', '15', '22'), fontsize='large')
    #pyplot.yticks(range(0, 20, 4), fontsize='large')
    #pyplot.ylim(0, 16)
    pyplot.yticks([5, 10, 15, 20, 25, 30, 35], fontsize='large')
    pyplot.ylim(0, 37)
    pyplot.legend( (p1[0], p2[0]), ('Dynamic', 'Fixed'),prop={'size':12} )
    pyplot.tight_layout(pad=0)
    pyplot.show()

def plot_HITs():
    plotx = []
    ploty = []
    for i in range(1, 82):
        filename = 'preprocess/{}.txt'.format(i)
        try:
            file = open(filename, 'r')
        except IOError:
            #print filename, 'not found'
            continue
        data = []
        for line in file:
            data.append(line)
        start = int(data[0])
        end = int(data[1])
        completed = int(data[2])
        reward = float(data[4])
        cate = int(data[7])
        time = int(data[8])
        if cate == 5:
            ploty.append(reward / time)
            plotx.append(completed * 1.0 / (end - start) * time * 3)
            #print reward / time, completed * 1.0 / (end - start) * time, i
    
    ls = numpy.polyfit(ploty, map(math.log, plotx), 1)
    p = numpy.poly1d(ls)
    print p
    pyplot.figure(figsize=(3.5,2.5))
    pyplot.xlabel('workload/hour(sec/h)', fontsize='large')
    pyplot.xticks([1e2,1e3,1e4,1e5], fontsize='large')
    pyplot.yticks(numpy.arange(0, 0.006, 0.001), fontsize='large')
    pyplot.ylim(0, 0.0035)
    pyplot.ylabel('wage/sec($/sec)', fontsize='large')
    pyplot.semilogx(plotx, ploty, 'bo')
    pyplot.tight_layout(pad=0)
    pyplot.show()
    
def plot_trend_N_and_NT():
    N = [200, 250, 300, 350, 400]
    reduction_NT_12 = [12, 9, 9, 8, 7]
    reduction_NT_24 = [22, 17, 12, 10, 9]
    reduction_NT_36 = [29, 20, 18, 14, 13]
    reduction_NT_48 = [67, 41, 32, 24, 21]
    pyplot.figure(figsize=(5,3))
    pyplot.xlabel('Total Number of Tasks', fontsize='large')
    pyplot.ylabel('Total Cost Reduction', fontsize='large')
    pyplot.xticks(N, fontsize='large')
    pyplot.yticks(range(0, 75, 10), fontsize='large')    
    pyplot.ylim(0, 70)
    curve_NT_12 = pyplot.plot(N, reduction_NT_12, 'bo-')
    curve_NT_24 = pyplot.plot(N, reduction_NT_24, 'rs--')
    curve_NT_36 = pyplot.plot(N, reduction_NT_36, 'y*-.')
    curve_NT_48 = pyplot.plot(N, reduction_NT_48, 'kv:')
    pyplot.legend( (curve_NT_12[0], curve_NT_24[0], curve_NT_36[0], curve_NT_48[0]), ('T=12h', 'T=24h', 'T=36h', 'T=48h'), prop={'size':12} )
    pyplot.tight_layout(pad=0)
    pyplot.show()
    
    #NT = [12, 24, 36, 48]
    #reduction_NT = [12, 22, 29, 67]
    #pyplot.xlabel('Total Time before Deadline(hour)')
    #pyplot.ylabel('Total Cost Reduction(Percentage)')    
    #pyplot.plot(NT, reduction_NT, 'bo-')
    #pyplot.show()
    
    #mul = [0.5, 1, 1.5, 2]
    #reduction_mul = [27, 22, 18, 21]
    #pyplot.xlabel('Scale Parameter')
    #pyplot.ylabel('Total Cost Reduction(Percentage)')    
    #pyplot.plot(mul, reduction_mul, 'bo-')
    #pyplot.show()

def plot_sensitivity_new():
    scale = [11, 13, 15, 17, 19]
    ave_scale = [9.38, 10.82, 12.37, 14.07, 15.91]
    remain_dyn_scale = [0, 2e-5, 8e-4, 0.02, 0.3]
    remain_fix_12_scale = [0, 0.214, 5.7, 18.4, 31.2]
    remain_fix_13_scale = [0, 0.007, 1.3, 9.5, 22.0]
    remain_fix_14_scale = [0, 0, 0.15, 3.5, 13.6]
    remain_fix_15_scale = [0, 0, 0.007, 0.8, 6.7]
    remain_fix_16_scale = [0, 0, 0, 0.12, 2.4]
    #lb_scale = [13.91, 18.55, 23.19, 27.83, 32.46]

    bias = [-0.79, -0.59, -0.39, -0.19, 0.01]
    ave_bias = [7.81, 9.71, 12.37, 15.66, 19.54]
    remain_dyn_bias = [0, 5e-5, 8e-4, 0.012, 0.195]
    remain_fix_12_bias = [0, 0.0054, 5.7, 36.29, 66.10]
    remain_fix_13_bias = [0, 0.0001, 1.3, 25.23, 56.55]
    remain_fix_14_bias = [0, 0, 0.15, 14.38, 46.75]
    remain_fix_15_bias = [0, 0, 0.007, 5.6, 36.25]
    remain_fix_16_bias = [0, 0, 0, 1.2652, 25.14]
    #lb_bias = [10.69, 16.94, 23.19, 29.44, 35.69]

    total = [1600, 1800, 2000, 2200, 2400]
    ave_total = [9.446, 10.891, 12.36, 13.861, 15.10]
    remain_dyn_total = [6e-5, 1e-5, 8e-4, 0.00329, 0.00983]
    remain_fix_12_total = [0.0013, 0.4154, 5.7, 18.51, 33.37]
    remain_fix_13_total = [0, 0.0364, 1.3, 8.79, 22.14]
    remain_fix_14_total = [0, 0.0017, 0.15, 2.633, 11.62]
    remain_fix_15_total = [0, 0, 0.007, 0.3865, 4.1338]
    remain_fix_16_total = [0, 0, 0, 0.0285, 0.7612]
    #lb_total = [17.61, 20.55, 23.19, 25.57, 27.75]

    #pyplot.figure(figsize=(4,2.8))
    #pyplot.xlabel('s parameter value', fontsize='large')
    #pyplot.xticks(range(11, 21, 2), fontsize='large')
    #pyplot.ylabel('Average Task Reward', fontsize='large')
    #pyplot.yticks(range(9, 17, 1), fontsize='large')
    #pyplot.ylim(9, 16)
    #pyplot.plot(scale, ave_scale, 'bo-')
    #pyplot.ylabel('Remaining Tasks', fontsize='large')
    #l1 = pyplot.plot(scale, remain_dyn_scale, 'bo-')
    #l2 = pyplot.plot(scale, remain_fix_12_scale, 'rs--')
    #l3 = pyplot.plot(scale, remain_fix_13_scale, 'y*-.')
    #l4 = pyplot.plot(scale, remain_fix_14_scale, 'c^:')
    #l5 = pyplot.plot(scale, remain_fix_15_scale, 'kv-')
    #l6 = pyplot.plot(scale, remain_fix_16_scale, 'mp--')
    #pyplot.yticks(range(0, 40, 5), fontsize='large')
    #pyplot.legend( (l1[0], l2[0], l3[0], l4[0], l5[0], l6[0]), ('Dynamic', 'Fixed-12', 'Fixed-13', 'Fixed-14', 'Fixed-15', 'Fixed-16'), loc = 2, prop={'size':10} )
    #pyplot.tight_layout(pad=0)
    #pyplot.show()    

    #pyplot.figure(figsize=(4,2.8))
    #pyplot.xlabel('b parameter value', fontsize='large')
    #pyplot.xticks(numpy.arange(-0.8, 0.2, 0.2), fontsize='large')
    #pyplot.xlim(-0.8, 0.01)
    #pyplot.ylabel('Average Task Reward', fontsize='large')
    #pyplot.yticks(range(7, 21, 2), fontsize='x-large')
    #pyplot.ylim(7, 20)
    #pyplot.plot(bias, ave_bias, 'bo-')
    #pyplot.ylabel('Remaining Tasks', fontsize='large')
    #l1 = pyplot.plot(bias, remain_dyn_bias, 'bo-')
    #l2 = pyplot.plot(bias, remain_fix_12_bias, 'rs--')
    #l3 = pyplot.plot(bias, remain_fix_13_bias, 'y*-.')
    #l4 = pyplot.plot(bias, remain_fix_14_bias, 'c^:')
    #l5 = pyplot.plot(bias, remain_fix_15_bias, 'kv-')
    #l6 = pyplot.plot(bias, remain_fix_16_bias, 'mp--')
    #pyplot.yticks(range(0, 80, 10), fontsize='large')
    #pyplot.legend( (l1[0], l2[0], l3[0], l4[0], l5[0], l6[0]), ('Dynamic', 'Fixed-12', 'Fixed-13', 'Fixed-14', 'Fixed-15', 'Fixed-16'), loc = 2, prop={'size':10} )
    #pyplot.tight_layout(pad=0)
    #pyplot.show()    

    pyplot.figure(figsize=(4,2.8))
    pyplot.xlabel('M parameter value', fontsize='large')
    pyplot.xticks(numpy.arange(1600, 2600, 200), fontsize='large')
    #pyplot.ylabel('Average Task Reward', fontsize='large')
    #pyplot.yticks(range(9, 17, 2), fontsize='large')
    #pyplot.ylim(9, 15.1)
    #pyplot.plot(total, ave_total, 'bo-')
    pyplot.ylabel('Remaining Tasks', fontsize='large')
    l1 = pyplot.plot(total, remain_dyn_total, 'bo-')
    l2 = pyplot.plot(total, remain_fix_12_total, 'rs--')
    l3 = pyplot.plot(total, remain_fix_13_total, 'y*-.')
    l4 = pyplot.plot(total, remain_fix_14_total, 'c^:')
    l5 = pyplot.plot(total, remain_fix_15_total, 'kv-')
    l6 = pyplot.plot(total, remain_fix_16_total, 'mp--')
    pyplot.yticks(range(0, 40, 5), fontsize='large')
    pyplot.legend( (l1[0], l2[0], l3[0], l4[0], l5[0], l6[0]), ('Dynamic', 'Fixed-12', 'Fixed-13', 'Fixed-14', 'Fixed-15', 'Fixed-16'), loc = 2, prop={'size':10} )
    pyplot.tight_layout(pad=0)
    pyplot.show()    
    
def plot_hit_completion():
    #Read arrival rate data from file
    infile = open('time_complete.log', 'r')
    arrival = []
    for line in infile:
        arrival.append(int(line))
        
    aggr = [sum(arrival[i * 18:(i+1)*18]) for i in range(0, 28*4)]
    pyplot.figure(figsize=(6, 3))
    pyplot.plot(numpy.arange(1, 29, 0.25), aggr, 'b-')
    pyplot.xlabel('Date', fontsize='large')
    pyplot.ylabel('Num of HIT completed', fontsize='large')
    pyplot.yticks([1e4,3e4,5e4,7e4,9e4], fontsize='large')
    pyplot.xticks([1, 8, 15, 22, 29], fontsize='large')
    pyplot.xlim(1, 29)
    pyplot.tight_layout(pad=0)
    pyplot.show()
    
if __name__ == '__main__':
    discrete_choice()
    #plot_trend_N_and_NT()
    #plot_HITs()
    #prepare_data()
    #fixed_deadline_simulation()
    #fixed_deadline_simulation_with_bound()
    #sensitivity_simulation()
    #plot_price_remain()
    #plot_sensitivity()
    #plot_sensitivity_2()
    #plot_sensitivity_new()
    #plot_para()
    #static_simulate()
    #plot_granularity()
    #plot_time_sensitivity()
    #static_simulation()
    #plot_static()
    #plot_hit_completion()
