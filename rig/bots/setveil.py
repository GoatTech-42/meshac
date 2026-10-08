import json,sys
json.dump({"veilXray":sys.argv[1]=="true"},open("/srv/config/meshac.json","w"))
