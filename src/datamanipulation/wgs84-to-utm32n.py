from pyproj import Proj
import numpy as np
import pandas as pd
from pandas import DataFrame


wgs84_to_utm32n = Proj("+proj=utm +zone=32N, +ellps=WGS84 +datum=WGS84 +units=m +no_defs")


intersections = pd.read_csv("denmark-intersections.csv")
lats = intersections["lat"]
lons = intersections["lon"]


UTMx, UTMy = wgs84_to_utm32n(lons, lats)
# print( DataFrame(np.c_[UTMx, UTMy], columns=['UTMx', 'UTMy']) )

out = pd.DataFrame({"UTMx": UTMx, "UTMy": UTMy, "wayID": intersections["wayID"], "oneway": intersections["oneway"]})
# print(out)
# print(intersections)

print("Saving to csv...")
out.to_csv("denmark-intersections-utm32.csv", index=False)

