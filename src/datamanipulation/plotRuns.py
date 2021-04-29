
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

DRIVEN_LEN_SCATTER = "Driven length (meters)"
NODES_IN_PATH = "Path length (#nodes)"
DRIVEN_LEN_AVG_LINE = "Average driven length (meters)"

##################################################################

plot_type = DRIVEN_LEN_AVG_LINE
file_in = "log-1000-aarhus-silkeborg-intersections.csv"
sections = 20

##################################################################

fig = plt.figure(figsize=(10, 7))
axes = fig.add_subplot(1,1,1)#, aspect='equal')
axes.set_ylabel("Expanded edges")
axes.set_xlabel(plot_type)

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
colors = {"TradDijk   ": "xkcd:baby poop green", 
          "OurDijk    ": "red",
          "BidirecDijk": "blue",
          "A*         ": "cyan",
          "ALT        ": "yellow",
          "BidrecAstar": "magenta",
          "BidrecALT  ": "lime"}
p1 = mpatches.Patch(color='xkcd:baby poop green', label="TradDijk   ")
p2 = mpatches.Patch(color='red', label="OurDijk    ")
p3 = mpatches.Patch(color='blue', label="BidirecDijk")
p4 = mpatches.Patch(color='cyan', label="A*         ")
p5 = mpatches.Patch(color='yellow', label="ALT        ")
p6 = mpatches.Patch(color='magenta', label="BidrecAstar")
p7 = mpatches.Patch(color='lime', label="BidrecALT  ")


markers = {"TradDijk   ": "x", 
          "OurDijk    ": "1",
          "BidirecDijk": "2",
          "A*         ": "3",
          "ALT        ": "4",
          "BidrecAstar": "8",
          "BidrecALT  ": "H"}

# set up for handles declaration
patches = [p1, p2, p3, p4, p5, p6, p7]

# define and place the legend
#legend = ax.legend(handles=patches,loc='upper right')

# alternative declaration for placing legend outside of plot
legend = axes.legend(handles=patches,loc='upper left')


# plot the whole data frame
#  csv:  algo, time, edges_expanded, no_nodes

# for count, algo in enumerate(data["algo"]):
#     print(str(count) + ": " + colors[algo])

if (plot_type == NODES_IN_PATH):
    axes.scatter(data["no_nodes"], data["edges_expanded"], s=10, 
        alpha=0.7,
        c=data["algo"].apply(lambda x: colors[x]),
        marker=".")
elif (plot_type == DRIVEN_LEN_SCATTER):
    pass

elif (plot_type == DRIVEN_LEN_AVG_LINE):
    max_len = max(data["driven_len"])
    lengths =       [None] * (len(colors) * sections)
    average_edges = [None] * (len(colors) * sections)
    for algo in range(len(colors)):
        for section in range(sections):
            lower = max_len/sections * section
            upper = max_len/sections * (section+1)

            index = len(colors) * section + algo
            lengths[index] = upper
            num_data_points = 0
            sum_data_points = 0
            for i in range(algo, len(data), len(colors)):
                x = data["driven_len"][i]
                if (lower < x and x < upper):
                    num_data_points += 1
                    sum_data_points += data["edges_expanded"][i]

            average_edges[index] = sum_data_points / num_data_points

    for algo in range(len(colors)):
        xs = []
        ys = []
        for section in range(sections):
            index = len(colors) * section + algo
            xs.append(lengths[index])
            ys.append(average_edges[index])

        axes.plot(xs, ys,
            alpha=0.7,
            c=list(colors.values())[algo],
            marker=".")


plt.show()

