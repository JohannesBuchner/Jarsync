## Process this file with automake to produce Makefile.in.
# Makefile for Jarsync test suite.
# $Id$

JAVAC = @JAVAC@
JAVACFLAGS = @CHECKJAVACFLAGS@
JAVA = @JAVA@
JAVAFLAGS = @JAVAFLAGS@
SHELL = @SHELL@

EXTRA_DIST = $(mauve_sources) $(test_sources) $(srcdir)/choose-classes \
  $(srcdir)/gnu/testlet/config.java.in

mauve_sources = \
  gnu/testlet/ResourceNotFoundException.java \
  gnu/testlet/SimpleTestHarness.java \
  gnu/testlet/TestHarness.java \
  gnu/testlet/Testlet.java

mauve_classes = $(patsubst %.java,classes/%.class,$(mauve_sources))

test_sources = \
  gnu/testlet/org/metastatic/rsync/SimpleTest.java \
  gnu/testlet/org/metastatic/rsync/SimpleTest2.java \
  gnu/testlet/org/metastatic/rsync/TestOfChecksum32.java \
  gnu/testlet/org/metastatic/rsync/TestOfBrokenMD4.java \
  gnu/testlet/org/metastatic/rsync/TestOfMD4.java

test_classes = $(patsubst %.java,classes/%.class,$(test_sources))

check-local: classes $(mauve_classes) $(test_classes) $(srcdir)/test_classes \
	     classes/gnu/testlet/config.class
	CLASSPATH=$$CLASSPATH:../classes/:classes/ \
	  $(JAVA) $(JAVAFLAGS) gnu.testlet.SimpleTestHarness \
	  -verbose < $(srcdir)/test_classes

$(srcdir)/test_classes: FORCE
	cd $(srcdir) && $(SHELL) choose-classes . JARSYNC

FORCE:

classes/gnu/testlet/config.class: gnu/testlet/config.java
	$(JAVAC) $(JAVACFLAGS) -d classes $<

$(mauve_classes): classes/%.class: $(srcdir)/%.java
	$(JAVAC) $(JAVACFLAGS) -d classes $<

$(test_classes): classes/%.class: $(srcdir)/%.java
	$(JAVAC) $(JAVACFLAGS) -d classes $<

classes:
	mkdir classes

clean-local:
	rm -f $(mauve_classes) $(test_classes) classes/gnu/testlet/config.class
