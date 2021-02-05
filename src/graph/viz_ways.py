
import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import matplotlib.colors as colors
from matplotlib import patches
from matplotlib.widgets import Button
import pandas as pd
import hashlib


NE = (10.2050, 56.1850)
SE = (10.2050, 56.1600)
SW = (10.1700, 56.1600)
NW = (10.1700, 56.1850)

fig = plt.figure(figsize=(7, 9)) ## Approximate ratio of first data set
axes = fig.add_subplot(1,1,1)#, aspect='equal')

axes.set_xlim([10.1700, 10.2050])
axes.set_ylim([56.1600, 56.1850])


#img_array = plt.imread("lisbon_2.jpg")
#axes.imshow(img_array)

xmax = axes.get_xlim()[1]
ymin = axes.get_ylim()[0]  # the y coordinates are reversed, ymax=0

# print(axes.get_xlim(), xmax)
# print(axes.get_ylim(), ymin)


def coordinatesOnFigure(long, lat, SW=SW, NE=NE, xmax=xmax, ymin=ymin):
    px = xmax/(NE[0]-SW[0])
    qx = -SW[0]*xmax/(NE[0]-SW[0])
    py = -ymin/(NE[1]-SW[1])
    qy = NE[1]*ymin/(NE[1]-SW[1])
    return px*long + qx, py*lat + qy



all_roads = pd.read_csv("all-roads.csv")
intersect = pd.read_csv("intersections.csv")


def hashAndColorfy(id):
        hashed = hashlib.md5( str(id).encode() ).hexdigest()
        h = abs( int(hashed, 16) ) 
        return ((int(str(h)[:3])%256)/256, (int(str(h)[4:7])%256)/256, (int(str(h)[8:11])%256)/256)



def plotPoints():
    print("--> plotting points")
    intersect["wayID"] = intersect["wayID"].map(lambda id : hashAndColorfy(id) )
    print("  --> points colored")

    # plot the whole data frame
    axes.scatter(intersect["lon"], intersect["lat"], alpha=0.7, s=2, c=intersect["wayID"], alpha=1)



def plotLines():
    print("--> plotting lines")
    # Overall idea: currently in the csv, nodes come in ordered from the "way"s.
    # So just check the wayID, and when it changes we know we hit a new way.
    lines = pd.DataFrame(data=None, columns=["x1", "y1", "x2", "y2", "color"])
    prev_wayID = None
    prev_x = prev_y = None

    print("  --> calculating points")
    for i in range(all_roads.shape[0]):
        if all_roads["wayID"][i] != prev_wayID:
            # new way, start over again
            prev_x, prev_y = all_roads["lon"][i], all_roads["lat"][i]
            prev_wayID = all_roads["wayID"][i]
            continue
        else:
            # Same road, continue
            curr_x, curr_y = all_roads["lon"][i], all_roads["lat"][i]
            color = hashAndColorfy(all_roads["wayID"][i])

            new_row = {"x1": prev_x, "y1": prev_y, "x2": curr_x, "y2": curr_y, "color": color}
            lines = lines.append(new_row, ignore_index=True) ## TODO SUPER SLOW
            
            prev_x, prev_y = curr_x, curr_y


    # DRAW!
    print("  --> drawing lines")
    for i in range(lines.shape[0]):
        # x is an array of [x1, x2], y an array of [y1, y2]
        x, y = [lines["x1"][i], lines["x2"][i]], [lines["y1"][i], lines["y2"][i]]
        plt.plot(x, y, alpha=0.7, linewidth=0.5, c=lines["color"][i]) ##



plotPoints()
plotLines()

print("--> saving as svg")
plt.axis('off')
plt.gca().set_position([0, 0, 1, 1])
plt.savefig("combined.svg")

plt.show()

print("--> DONE")


