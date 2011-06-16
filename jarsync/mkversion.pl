#!/usr/bin/perl

my $version = "PRERELEASE";
my $vdouble = "0.500";

my $svnrev = `/opt/local/bin/svnversion -n .`;

($svnrev =~ m/(\d+)[MS]*$/) && ($svnrev = $1);

open(FH, "src/main/java/org/metastatic/rsync/version.java.in") or die "$0: opening version.java.in: $!";
my $in = join("", <FH>);
close(FH);

my $product_version = $version . " (svn " . $svnrev . ")";

$in =~ s/PACKAGE_VERSION/$product_version/;
$in =~ s/DOUBLE_VERSION/$vdouble/;

print STDOUT $in;

open(FH, ">src/main/java/org/metastatic/rsync/version.java") or die "$0: opening version.java: $!";
print FH $in;
close(FH);
