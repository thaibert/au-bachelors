
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



df = pd.read_csv("raw.csv")
print(df.head())

def hashAndColorfy(id):
    hashed = hashlib.md5( str(id).encode() ).hexdigest()
    h = abs( int(hashed, 16) ) 
    return ((int(str(h)[:3])%256)/256, (int(str(h)[4:7])%256)/256, (int(str(h)[8:11])%256)/256)

df["wayID"] = df["wayID"].map(lambda id : hashAndColorfy(id) )

print(df.head())

# plot the whole DF
axes.scatter(df["lon"], df["lat"], s=2, c=df["wayID"], alpha=1)

plt.show()
