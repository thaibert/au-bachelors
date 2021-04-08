
import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import matplotlib.colors as colors
from matplotlib import patches
from matplotlib.widgets import Button
import pandas as pd
import hashlib
from matplotlib import colors as mcolors




fig = plt.figure(figsize=(10, 7))
axes = fig.add_subplot(1,1,1)#, aspect='equal')
axes.set_ylabel("Expanded edges")
axes.set_xlabel("Path length (#nodes)")

#axes.set_xlim([MIN_LON, MAX_LON])
#axes.set_ylim([MIN_LAT, MAX_LAT])


data = pd.read_csv("data-log.csv")


def hashAndColorfy(id):
    if id == "TradDijk   ":
        return "xkcd:baby poop green"
    elif id == "OurDijk    ":
        return "red"
    elif id == "BidirecDijk":
        return "blue"
    elif id == "A*         ":
        return "cyan"
    elif id == "ALT        ":
        return "yellow"
    elif id == "BidrecAstar":
        return "dimgrey"
    elif id == "BidrecALT  ":
        return "lime"
    
    # hashed = hashlib.md5( str(id).encode() ).hexdigest()
    # h = abs( int(hashed, 16) ) 
    # return ((int(str(h)[:3])%256)/256, (int(str(h)[4:7])%256)/256, (int(str(h)[8:11])%256)/256)

data["algo"] = data["algo"].map(lambda id : hashAndColorfy(id) )


# print(data)


# plot the whole data frame
# "algo, time, edges_expanded, no_nodes,\n");

axes.scatter(data["no_nodes"], data["edges_expanded"], s=10, c=data["algo"])
plt.show()

