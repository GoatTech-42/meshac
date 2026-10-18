# Builds the movement test course on the rig server. One lane per scenario, spaced 10 blocks apart along x. Flat surface is y=-61, standing height -60.
. "$(dirname "$0")/lib.sh"
rc "forceload add 96 -8 224 60" >/dev/null
rc "fill 96 -60 -8 224 -20 60 air" >/dev/null
rc "fill 108 -61 -4 112 -53 4 stone" >/dev/null                      # fall: 8-block pillar, edge at z=5
rc "fill 118 -62 3 122 -61 40 water" >/dev/null                      # water lane
rc "fill 127 -60 6 133 -52 7 stone" >/dev/null                       # wall
rc "fill 138 -60 6 142 -48 7 stone" >/dev/null                       # ladder wall
rc "fill 140 -60 5 140 -48 5 ladder[facing=north]" >/dev/null
rc "fill 148 -60 6 152 -60 20 stone" >/dev/null                      # one-block step
rc "fill 169 -60 6 171 -59 30 cobweb" >/dev/null                     # web
rc "fill 178 -61 3 182 -61 40 soul_sand" >/dev/null                  # soul sand
rc "fill 188 -61 3 192 -61 40 powder_snow" >/dev/null                # powder snow
echo world ready
