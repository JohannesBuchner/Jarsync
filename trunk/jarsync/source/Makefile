# Makefile for Jarsync.
# $Id$

#cc     = javac
cc = jikes
#ccopts = -O
#ccopts = -g -source 1.4
ccopts = -g

sources = org/metastatic/rsync/ChecksumPair.java \
  org/metastatic/rsync/Configuration.java \
  org/metastatic/rsync/DataBlock.java \
  org/metastatic/rsync/Delta.java \
  org/metastatic/rsync/Generator.java \
  org/metastatic/rsync/MD4.java \
  org/metastatic/rsync/Matcher.java \
  org/metastatic/rsync/MessageDigest.java \
  org/metastatic/rsync/Offsets.java \
  org/metastatic/rsync/Rebuilder.java \
  org/metastatic/rsync/RollingChecksum.java \
  org/metastatic/rsync/RsyncConstants.java \
  org/metastatic/rsync/TwoKeyMap.java
classes = $(patsubst %.java,%.class,$(sources))
inner = org/metastatic/rsync/Rebuilder\$$OffsetComparator.class \
  org/metastatic/rsync/Rebuilder\$$TopologicalSorter.class \
  org/metastatic/rsync/TwoKeyMap\$$1\$$c.class \
  org/metastatic/rsync/TwoKeyMap\$$1.class \
  org/metastatic/rsync/TwoKeyMap\$$2\$$c.class \
  org/metastatic/rsync/TwoKeyMap\$$2.class \
  org/metastatic/rsync/TwoKeyMap\$$3\$$c.class \
  org/metastatic/rsync/TwoKeyMap\$$3.class \
  org/metastatic/rsync/TwoKeyMap\$$StrongKey.class \
  org/metastatic/rsync/TwoKeyMap\$$SubTable.class

all: jarsync.jar

jarsync.jar: $(classes)
	jar cvf jarsync.jar $(classes) $(inner)

$(classes): %.class: %.java
	$(cc) $(ccopts) $<

clean:
	rm -f $(classes) $(inner)
