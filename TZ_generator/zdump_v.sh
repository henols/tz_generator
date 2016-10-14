while read p; do
  zdump -v $p
done <TimeZones.txt >zdump_v.txt