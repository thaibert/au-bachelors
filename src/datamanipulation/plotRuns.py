
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



fig = plt.figure(figsize=(10, 7))
axes = fig.add_subplot(1,1,1)#, aspect='equal')
axes.set_ylabel("Expanded edges")
axes.set_xlabel("Path length (#nodes)")

#axes.set_xlim([MIN_LON, MAX_LON])
#axes.set_ylim([MIN_LAT, MAX_LAT])


data = pd.read_csv("data-log.csv")


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
          "BidrecAstar": "dimgrey",
          "BidrecALT  ": "lime"}
p1 = mpatches.Patch(color='xkcd:baby poop green', label="TradDijk   ")
p2 = mpatches.Patch(color='red', label="OurDijk    ")
p3 = mpatches.Patch(color='blue', label="BidirecDijk")
p4 = mpatches.Patch(color='cyan', label="A*         ")
p5 = mpatches.Patch(color='yellow', label="ALT        ")
p6 = mpatches.Patch(color='dimgrey', label="BidrecAstar")
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

axes.scatter(data["no_nodes"], data["edges_expanded"], s=2000, 
    c=data["algo"].apply(lambda x: colors[x]))
plt.show()

