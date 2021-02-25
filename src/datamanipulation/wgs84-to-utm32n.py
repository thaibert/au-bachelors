from pyproj import Proj
import numpy as np
from pandas import DataFrame


wgs84_to_utm32n = Proj("+proj=utm +zone=32N, +ellps=WGS84 +datum=WGS84 +units=m +no_defs")

# print("\n\n\n\n")
lons = (10.1722176, 10.5622452)
lats = (56.1634686, 57.7262073 )


UTMx, UTMy = wgs84_to_utm32n(lons, lats)
print( DataFrame(np.c_[UTMx, UTMy], columns=['UTMx', 'UTMy']) )

