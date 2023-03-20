# -*- coding: utf-8 -*-

import pandas as pd
import csv
import matplotlib.pyplot as plt
import seaborn as sns
import scipy.stats
from scipy import stats
import numpy as np


def read_CSV_andPlot(path):
    

    df = pd.read_csv(path,names=["Steps","Objects Identified","Error"])
    
    print(df)

    color_dict =['green','red'] #['Objects Identified': 'green', 'Error': 'red']
    
    df.plot(x="Steps",y= ["Objects Identified","Error"],color=color_dict)
    plt.ylabel("Percentage")
    plt.show()
    
    
    



def mean_confidence_interval(data, confidence=0.95):# its for T
    a = 1.0 * np.array(data)
    #print(a)
    n = len(a)
    m, se = np.mean(a), scipy.stats.sem(a)

    h = se * scipy.stats.t.ppf((1 + confidence) / 2., n-1)
    return m, m-h, m+h








pathFileCSV="C:/Users/gusta/git/AI_project1/zWorkonthis/stats.csv"

read_CSV_andPlot(pathFileCSV)



dataObj=[700,650, 700,600,400]
dataErr=[4.3, 3, 9.4,2.5,2.02]
print(mean_confidence_interval(dataObj))
print(mean_confidence_interval(dataErr))

""" 7runs or 5 runs
Random5agts
    dataObj=[1250,1350,1350,850,900,850,1000]
    dataErr=[14.8, 22.6,4.1,11.8,12.2,6.6,6.8]

Random1agt
    dataObj=[4750, 4450,3550,4250,5000,5000,4150] #5000 it means that it was at 95% of coverage
    dataErr=[5.8, 7.0 , 21.7, 6.6, 5.8,2.7,13.9]
    
    
Random5agtsmode2_5splits
    dataObj=[1050,1300,1500,950,1350] 
    dataErr=[9.9,4.8, 10.4, 7.4, 1.8]
    
Random5agtsmode2_8splits
    dataObj=[800, 900,800,1000,700] 
    dataErr=[25.5,6.5,25.6, 4.9,9.1]

Random5agtsmode2_20splits
dataObj=[800, 1050,1000,900,950] 
dataErr=[5.4,12.5, 9.13, 7,8.1]


Structured5agtsmode2_5splits
dataObj=[900, 1300,1050,1750,1900] 
dataErr=[33.2,30.2,51.5,29.7,6.6]

Reclasssification5agts
dataObj=[900, ] 
dataErr=[23]


NN_v1, 2000 epochs
dataObj=[600,700,700, 800,900]
dataErr=[16.0, 23,20.6,3.3,23]

NN_v1, 10000 epochs
dataObj=[700,900,600, 500,700]
dataErr=[4.9, 8.4,0.54,4.8,12.9]

NN_v3struct2K
dataObj=[800,700,600,850 ,800]
dataErr=[17.5,15.9 ,13.5,10.13,4.3]

NN_V3struct10kepoch
dataObj=[700,650, 700,600,400]
dataErr=[4.3, 3, 9.4,2.5,2.02]



"""