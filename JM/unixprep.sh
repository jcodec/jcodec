#!/bin/sh

which sed || (echo "sed unavailable" 1>&2; exit 1)

echo "Removing DOS LF chars..."
for f in l[ed][ne]cod/[si][rn]c/*.[ch] bin/*.cfg
do
   sed -e "s///" < $f >$f.tmp && mv $f.tmp $f
done

for f in l[ed][ne]cod/Makefile
do
   sed -e "s///" < $f >$f.tmp && mv $f.tmp $f
done

for f in rtpdump/*.cpp rtpdump/Makefile
do
   sed -e "s///" < $f >$f.tmp && mv $f.tmp $f
done

for f in rtp_loss/*.cpp rtp_loss/*.h rtp_loss/Makefile
do
   sed -e "s///" < $f >$f.tmp && mv $f.tmp $f
done


echo "Done."

