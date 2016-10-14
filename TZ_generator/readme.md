#Generate time zone and daylight savings data.#

The TZ generator can generate TZ data chunks and a full C file with all the data in it.

Add all the required references to the locations you want to generate data for in the file `TimeZones.txt`.

Run the `zdump_v.sh`   

A new file `zdump_v.txt` will be created.

Run the generator with desired arguments ex.

`java tt.GenerateTimeZonesData -src .\TZ_generator\zdump_v.txt -dst C:\tmp\tz_embed -alias .\TZ_generator\Aliases.txt`


 