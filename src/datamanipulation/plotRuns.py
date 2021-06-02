
import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import matplotlib.colors as colors
from matplotlib import patches
from matplotlib.widgets import Button
import pandas as pd
import hashlib
from matplotlib import patches as mpatches
from matplotlib import colors as mcolors
from matplotlib.ticker import FuncFormatter


NODES_SCATTER = "Path length (#nodes)"
NODES_IN_PATH = "Path length (#nodes)"
DRIVEN_LEN_AVG_LINE = "Average driven length (meters)"

##################################################################

plot_type = NODES_IN_PATH
# file_in = "log-1000-pruned-denmark-latest-roads.csv"
file_in = "log-1000-pruned-iceland-latest-roads.csv"
COUNTRY = "iceland"
sections = 10

##################################################################

fig = plt.figure(figsize=(10, 7))
axes = fig.add_subplot(1,1,1)#, aspect='equal')
# axes.set_ylabel("Expanded edges")
axes.set_ylabel("Visited nodes")
axes.set_xlabel(plot_type)

def millions(x, pos):
    'The two args are the value and tick position'
    return '%1.1fM' % (x*1e-6)
def kilos(x, pos):
    'The two args are the value and tick position'
    return '%1.1fK' % (x*1e-3)

formatter = None

if COUNTRY == "iceland":
    axes.set_ylim([0,   80000])
    formatter = FuncFormatter(kilos)
elif COUNTRY == "denmark":
    axes.set_ylim([0, 1200000])
    formatter = FuncFormatter(millions)

axes.yaxis.set_major_formatter(formatter)


#axes.set_xlim([MIN_LON, MAX_LON])
#axes.set_ylim([MIN_LAT, MAX_LAT])


data = pd.read_csv(file_in)


# def hashAndColorfy(id):
#     if id == "TradDijk   ":
#         return "xkcd:baby poop green"
#     elif id == "OurDijk    ":
#         return "red"
#     elif id == "BidirecDijk":
#         return "blue"
#     elif id == "A*         ":
#         return "cyan"
#     elif id == "ALT        ":
#         return "yellow"
#     elif id == "BidrecAstar":
#         return "dimgrey"
#     elif id == "BidrecALT  ":
#         return "lime"
    
#     # hashed = hashlib.md5( str(id).encode() ).hexdigest()
#     # h = abs( int(hashed, 16) ) 
#     # return ((int(str(h)[:3])%256)/256, (int(str(h)[4:7])%256)/256, (int(str(h)[8:11])%256)/256)

# data["algo"] = data["algo"].map(lambda id : hashAndColorfy(id) )



# ============ https://stackoverflow.com/a/46246771 ==================
# build the legend
colors = {"OurDijk    ": "xkcd:baby poop green",
          "TradDijk   ": "blue",     
          "BidirecDijk": "turquoise",  
          "A*         ": "red",
          "ALT        ": "xkcd:green",
          #################
          "BidrecAstar": "xkcd:orange",
          "BidrecALT  ": "xkcd:yellowgreen",
          ################
          "ReachDijk  ": "cyan",
          "ReachALT   ": "lime"}
# p0 = mpatches.Patch(color='xkcd:baby poop green', label="OurDijk    ")
p1 = mpatches.Patch(color='blue', label="Dijkstra")
p2 = mpatches.Patch(color='red', label="A*")
p3 = mpatches.Patch(color='xkcd:green', label="ALT")
#############
p4 = mpatches.Patch(color='turquoise', label="Bidirectional Dijkstra")
p5 = mpatches.Patch(color='xkcd:orange', label="Bidirectional A*")
p6 = mpatches.Patch(color='xkcd:yellowgreen', label="Bidirectional ALT")
##############
p7 = mpatches.Patch(color='cyan', label="Reach (dijkstra)")
p8 = mpatches.Patch(color='lime', label="Reach (ALT)")


# set up for handles declaration
patches = [p1, p2, p3, p4, p5, p6, p7, p8]


markers = ["<",  # Our dijkstra - IGNORE 
           ">", # Traditional dijkstra
            "v",  # Bidirec dijkstra
           "*",  # A*
           "s",  # ALT 
           "x",  # Bidirec A*
           "D", # Bidirec ALT
           "^", # Reach dijk
           "d"] # Reach ALT

names = ["Dijkstra (memory-saving)",
         "Dijkstra",
         "Bidirectional Dijkstra",
         "A*",
         "ALT",
         "Bidirectional A*",
         "Bidirectional ALT",
         "Reach (Dijkstra)",
         "Reach (ALT)"]

# define and place the legend
#legend = ax.legend(handles=patches,loc='upper right')

# alternative declaration for placing legend outside of plot
# legend = axes.legend(handles=patches,loc='upper left')


# plot the whole data frame
#  csv:  algo, time, edges_expanded, no_nodes

# for count, algo in enumerate(data["algo"]):
#     print(str(count) + ": " + colors[algo])

y_data = data["verticesScanned"]
x_data = None
if plot_type == DRIVEN_LEN_AVG_LINE:
    x_data = data["driven_len"]
elif plot_type == NODES_IN_PATH:
    x_data = data["no_nodes"]


# if (plot_type == NODES_SCATTER):
#     axes.scatter(data["no_nodes"], y_data, s=10, 
#         alpha=0.7,
#         c=data["algo"].apply(lambda x: colors[x]),
#         marker=".")
# else:
max_len = max(x_data)
lengths =       [None] * (len(colors) * sections)
average_y = [None] * (len(colors) * sections)
for algo in range(len(colors)):
    for section in range(sections):
        lower = max_len/sections * section
        upper = max_len/sections * (section+1)

        index = len(colors) * section + algo
        lengths[index] = upper
        num_data_points = 0
        sum_data_points = 0
        for i in range(algo, len(data), len(colors)):
            x = x_data[i]
            if (lower < x and x < upper):
                num_data_points += 1
                sum_data_points += y_data[i]
        average_y[index] = sum_data_points / (num_data_points + 0.00001) # avoiding divbyzero

for algo in range(len(colors)):
    xs = []
    ys = []
    for section in range(sections):
        index = len(colors) * section + algo
        xs.append(lengths[index])
        ys.append(average_y[index])
    axes.plot(xs, ys,
        alpha=0.7,
        c=list(colors.values())[algo],
        marker=markers[algo],
        label=names[algo])

axes.legend(loc="best")


plt.show()

