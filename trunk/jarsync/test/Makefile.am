## Process this file with automake to produce Makefile.in.
# Makefile for Jarsync test suite.
# $Id$

JAVAC = @JAVAC@
JAVACFLAGS = @JAVACFLAGS@
JAVA = @JAVA@
JAVAFLAGS = @JAVAFLAGS@
SHELL = @SHELL@

EXTRA_DIST = $(mauve_sources) $(test_sources) choose-classes \
  gnu/testlet/config.java.in

mauve_sources = \
  gnu/testlet/ResourceNotFoundException.java \
  gnu/testlet/SimpleTestHarness.java \
  gnu/testlet/TestHarness.java \
  gnu/testlet/Testlet.java \
  gnu/testlet/config.java

mauve_classes = $(patsubst %.java,../classes/%.class,$(mauve_sources))

test_sources = \
  gnu/testlet/org/metastatic/rsync/SimpleTest.java \
  gnu/testlet/org/metastatic/rsync/SimpleTest2.java \
  gnu/testlet/org/metastatic/rsync/TestOfChecksum32.java \
  gnu/testlet/org/metastatic/rsync/TestOfBrokenMD4.java \
  gnu/testlet/org/metastatic/rsync/TestOfMD4.java

test_classes = $(patsubst %.java,../classes/%.class,$(test_sources))

check-local: $(mauve_classes) $(test_classes) test_classes
	$(JAVA) $(JAVAFLAGS) gnu.testlet.SimpleTestHarness -verbose < test_classes

test_classes: FORCE
	$(SHELL) ./choose-classes . JARSYNC

FORCE:

$(mauve_classes): ../classes/%.class: %.java
	$(JAVAC) -source 1.4 $(JAVACFLAGS) $<

$(test_classes): ../classes/%.class: %.java
	$(JAVAC) -source 1.4 $(JAVACFLAGS) $<

clean-local:
	rm -f $(mauve_classes) $(test_classes)
