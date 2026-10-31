#!/usr/bin/perl
# For each mixin source, find method = "name(desc)ret" strings (incl. in arrays) and check the target class
# (from @Mixin(X.class) / targets=) declares that exact descriptor, via javap -s.
use strict; use warnings;
my ($src, $jp) = @ARGV;
my @files = split /\n/, `find "$src" -name '*.java' -path '*mixin*'`;
my %cache;
sub descs { my $c = shift; return $cache{$c} //= do { my $o = `"$jp" -p -s '$c' 2>&1`; my %d; my $n;
  for (split /\n/, $o) { if (/^\s+.*?\b([\w\$<>]+)\(.*\)/) { $n = $1; $n = '<init>' if $c =~ /(?:^|[.\$])\Q$n\E$/; } elsif (/^\s+descriptor: (\S+)/ && defined $n) { $d{"$n$1"} = 1; undef $n; } }
  \%d } }
for my $f (@files) {
  open my $fh, '<', $f or next; my $s = do { local $/; <$fh> }; close $fh;
  my %imp; while ($s =~ /^import\s+([\w.]+)\.(\w+);/mg) { $imp{$2} = "$1.$2"; }
  my ($pkg) = $s =~ /^package\s+([\w.]+);/m;
  # only top-level @Mixin
  my ($t) = $s =~ /\@Mixin\(\s*(?:value\s*=\s*)?\{?\s*([\w.]+)\.class/;
  my $cls;
  if ($t) { my @p = split /\./, $t; my $first = shift @p; my $base = $imp{$first} // ($first =~ /^[a-z]/ ? join('.', $first, @p) : "$pkg.$first"); $cls = @p && $base ne join('.', $first, @p) ? join('$', $base, @p) : $base; }
  elsif ($s =~ /\@Mixin\(\s*targets\s*=\s*"([^"]+)"/) { $cls = $1; $cls =~ s#/#.#g; }
  next unless $cls;
  my $d = descs($cls);
  while ($s =~ /method\s*=\s*(\{[^}]*\}|"[^"]*")/g) {
    my $m = $1;
    for my $one ($m =~ /"([^"]+)"/g) {
      next unless $one =~ /^([\w\$<>]+)(\(.*)$/;
      my $key = "$1$2";
      print "$f: $cls: $key not found\n" unless $d->{$key};
    }
  }
}
