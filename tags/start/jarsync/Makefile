# Makefile for Jarsync.
# $Id$

#cc     = javac
cc = jikes
#ccopts = -O
#ccopts = -g -source 1.4
ccopts = -g -classpath $$CLASSPATH:lib/jarsync.jar
jar    = jar

compress = bzip2
suffix = .bz2

sources = $(wildcard source/org/metastatic/rsync/*.java)
test_src = test.java

distfiles = $(sources) $(test_src) AUTHORS COPYING Makefile README TODO mutate.pl ChangeLog build.xml source/Makefile

version = 0.0.7
package = jarsync

distdir = $(package)-$(version)

all: lib/jarsync.jar test

lib/jarsync.jar: source/jarsync.jar
	-mkdir lib
	mv -f source/jarsync.jar lib

test: jarsync.jar test.class

test.class: test.java
	$(cc) $(ccopts) test.java

source/jarsync.jar:
	make -C source

dist: $(distdir)
	tar cf $(distdir).tar $(distdir)
	$(compress) -f $(distdir).tar
	jar cMf $(distdir).jar $(distdir)
	rm -rf $(distdir)

$(distdir):
	mkdir -p $(distdir)
	cp --parents -a $(distfiles) $(distdir)

clean:
	rm -f test.class
	make -C source clean
