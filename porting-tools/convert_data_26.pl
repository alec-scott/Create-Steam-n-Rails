#!/usr/bin/env perl
# Converts Railways' generated 1.20 data (src/generated/resources/data/railways) to 26.1 / Create Fly formats.
# Datagen is stubbed, so this stands in for the recipe, loot table and advancement providers.
#
# Run from the repo root after renaming the folders (recipes -> recipe, loot_tables -> loot_table,
# advancements -> advancement): perl porting-tools/convert_data_26.pl
#
# - recipes: 1.20 ingredient objects ({"item"}/{"tag"}) -> "id" / "#tag"; results {"item"} -> {"id"}; Create recipes
#   to Create Fly's codecs (mixing fluid_ingredients/fluid_results with fluid amounts in droplets, item_application
#   target/ingredient, sequenced_assembly $ingredient/$result steps, mechanical_crafting accept_mirrored); paint
#   NBT {Color} -> the railways:paint_color component; fabric:load_conditions to the single-condition form.
# - loot tables: match_tool enchantment predicates -> predicates/minecraft:enchantments.
# - advancements: inventory_changed item predicates and display icons.
# - internal item tags: 1.20 c:/forge: names -> 26.1 conventional tags.
use strict;
use warnings;
use File::Find;
use JSON::PP;

my $root = 'src/generated/resources/data/railways';
my $json = JSON::PP->new->utf8->canonical->pretty->indent_length(2)->space_before(0);
my %warned;

sub warn_once { my $m = shift; print STDERR "warning: $m\n" unless $warned{$m}++; }

sub read_json {
    my $file = shift;
    open my $in, '<:raw', $file or die "$file: $!";
    local $/;
    my $data = $json->decode(<$in>);
    close $in;
    return $data;
}

sub write_json {
    my ($file, $data) = @_;
    open my $out, '>:raw', $file or die "$file: $!";
    print $out $json->encode($data);
    close $out;
}

sub files_under {
    my $dir = shift;
    my @files;
    find(sub { push @files, $File::Find::name if /\.json$/ }, $dir) if -d $dir;
    return sort @files;
}

# --- recipes ---------------------------------------------------------------------------------------------------

sub ingredient {
    my $in = shift;
    return $in unless ref $in;
    if (ref $in eq 'ARRAY') {
        my @alts = map { ingredient($_) } @$in;
        return \@alts unless grep { /^#/ } @alts;
        # 26.1 ingredients can't list alternative tags; keep the first (e.g. iron nuggets over zinc nuggets)
        warn_once("alternative tags reduced to the first: @alts");
        return $alts[0];
    }
    return $in->{item} if exists $in->{item};
    return "#$in->{tag}" if exists $in->{tag};
    die 'unknown ingredient ' . $json->encode($in);
}

sub output {
    my $r = shift;
    return { id => $r } unless ref $r;
    die 'item result with nbt ' . $json->encode($r) if exists $r->{nbt};
    my %out = (id => $r->{item} // $r->{id});
    $out{count} = $r->{count} if ($r->{count} // 1) != 1;
    $out{chance} = $r->{chance} if exists $r->{chance} && $r->{chance} != 1;
    return \%out;
}

sub is_fluid { my $x = shift; return ref $x eq 'HASH' && (exists $x->{fluid} || exists $x->{fluidTag}); }

sub fluid_amount {
    my $f = shift;
    return int($f->{'railways:amount'} * 81000 / ($f->{'railways:amount:unit'} // 81000)) if exists $f->{'railways:amount'};
    return $f->{amount} // die 'fluid without amount ' . $json->encode($f);
}

sub fluid_components {
    my $f = shift;
    return undef unless exists $f->{nbt};
    my $nbt = $f->{nbt};
    my $color = ref $nbt ? $nbt->{Color} : ($nbt =~ /Color:"?(\w+)"?/ ? $1 : undef);
    die 'unsupported fluid nbt ' . $json->encode($f) unless defined $color;
    return { 'railways:paint_color' => $color };
}

sub fluid_ingredient {
    my $f = shift;
    if (exists $f->{fluidTag}) {
        return { type => 'fluid_tag', amount => fluid_amount($f), fluid_tag => "#$f->{fluidTag}" };
    }
    my %out = (type => 'fluid_stack', amount => fluid_amount($f), fluid => $f->{fluid});
    my $c = fluid_components($f);
    $out{components} = $c if $c;
    return \%out;
}

sub fluid_result {
    my $f = shift;
    my %out = (id => $f->{fluid}, amount => fluid_amount($f));
    my $c = fluid_components($f);
    $out{components} = $c if $c;
    return \%out;
}

sub load_conditions {
    my $c = shift;
    return $c if ref $c eq 'HASH';
    return $c->[0] if @$c == 1;
    return { condition => 'fabric:and', values => $c };
}

sub sequence_step {
    my $s = shift;
    my $type = $s->{type};
    if ($type eq 'create:deploying') {
        my %out = (type => $type, target => '$ingredient', ingredient => ingredient($s->{ingredients}[1]),
            results => ['$result']);
        $out{keep_held_item} = JSON::PP::true if $s->{keepHeldItem};
        return \%out;
    }
    if ($type eq 'create:pressing') {
        return { type => $type, ingredient => '$ingredient', results => ['$result'] };
    }
    if ($type eq 'create:cutting') {
        return { type => $type, ingredient => '$ingredient', results => ['$result'],
            processing_time => $s->{processingTime} // 50 };
    }
    die "unsupported sequence step $type";
}

sub convert_recipe {
    my $r = shift;
    my $type = $r->{type};
    my %out = (type => $type);
    $out{'fabric:load_conditions'} = load_conditions($r->{'fabric:load_conditions'}) if $r->{'fabric:load_conditions'};
    $out{category} = $r->{category} if exists $r->{category};
    $out{show_notification} = $r->{show_notification} if exists $r->{show_notification};

    if ($type eq 'minecraft:crafting_shaped' || $type eq 'create:mechanical_crafting') {
        $out{pattern} = $r->{pattern};
        $out{key} = { map { $_ => ingredient($r->{key}{$_}) } keys %{ $r->{key} } };
        $out{result} = output($r->{result});
        $out{accept_mirrored} = $r->{acceptMirrored} if exists $r->{acceptMirrored};
    } elsif ($type eq 'minecraft:crafting_shapeless') {
        $out{ingredients} = [ map { ingredient($_) } @{ $r->{ingredients} } ];
        $out{result} = output($r->{result});
    } elsif ($type eq 'minecraft:stonecutting') {
        $out{ingredient} = ingredient($r->{ingredient});
        my $result = output($r->{result});
        $result->{count} = $r->{count} if ($r->{count} // 1) != 1;
        $out{result} = $result;
    } elsif ($type eq 'create:mixing') {
        my @items = grep { !is_fluid($_) } @{ $r->{ingredients} };
        my @fluids = grep { is_fluid($_) } @{ $r->{ingredients} };
        my @item_results = grep { !is_fluid($_) } @{ $r->{results} };
        my @fluid_results = grep { is_fluid($_) } @{ $r->{results} };
        $out{ingredients} = [ map { ingredient($_) } @items ] if @items;
        $out{fluid_ingredients} = [ map { fluid_ingredient($_) } @fluids ] if @fluids;
        $out{results} = [ map { output($_) } @item_results ] if @item_results;
        $out{fluid_results} = [ map { fluid_result($_) } @fluid_results ] if @fluid_results;
        $out{heat_requirement} = $r->{heatRequirement} if exists $r->{heatRequirement};
        $out{processing_time} = $r->{processingTime} if exists $r->{processingTime};
    } elsif ($type eq 'create:item_application') {
        $out{target} = ingredient($r->{ingredients}[0]);
        $out{ingredient} = ingredient($r->{ingredients}[1]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
        $out{keep_held_item} = JSON::PP::true if $r->{keepHeldItem};
    } elsif ($type eq 'create:sequenced_assembly') {
        my @results = map { output($_) } @{ $r->{results} };
        $out{ingredient} = ingredient($r->{ingredient});
        $out{transitional_item} = { id => $r->{transitionalItem}{item} };
        $out{result} = shift @results;
        $out{junks} = \@results if @results;
        $out{loops} = $r->{loops} // 1;
        $out{sequence} = [ map { sequence_step($_) } @{ $r->{sequence} } ];
        die 'sequence must have at least two steps' if @{ $out{sequence} } * $out{loops} < 2;
    } else {
        die "unsupported recipe type $type";
    }
    return \%out;
}

# --- loot tables and advancements --------------------------------------------------------------------------------

# 1.20 item predicate ({"items": [...]} / {"tag"} / {"enchantments"}) -> 26.1
sub item_predicate {
    my $p = shift;
    my %out = %$p;
    if (exists $out{tag}) {
        $out{items} = '#' . delete $out{tag};
    } elsif (exists $out{item}) {
        $out{items} = delete $out{item};
    } elsif (ref $out{items} eq 'ARRAY' && @{ $out{items} } == 1) {
        $out{items} = $out{items}[0];
    }
    if (my $ench = delete $out{enchantments}) {
        $out{predicates}{'minecraft:enchantments'} =
            [ map { { enchantments => $_->{enchantment}, (exists $_->{levels} ? (levels => $_->{levels}) : ()) } } @$ench ];
    }
    return \%out;
}

sub walk {
    my ($node, $fn) = @_;
    if (ref $node eq 'HASH') {
        $fn->($node);
        walk($_, $fn) for values %$node;
    } elsif (ref $node eq 'ARRAY') {
        walk($_, $fn) for @$node;
    }
}

my @compat_mods = qw(biomesoplenty blue_skies byg create_dd hexcasting natures_spirit quark tfc twilightforest);

sub convert_loot_table {
    my ($t, $file) = @_;
    # blocks without an item (extensions, headstock bars) dropped minecraft:air, which 26.1 rejects
    for my $pool (@{ $t->{pools} // [] }) {
        $pool->{entries} = [ grep { !(($_->{type} // '') eq 'minecraft:item' && $_->{name} eq 'minecraft:air') } @{ $pool->{entries} } ];
    }
    $t->{pools} = [ grep { @{ $_->{entries} } } @{ $t->{pools} // [] } ];
    # compat tracks only exist when their mod is loaded (the recipes already carry the same condition)
    for my $mod (@compat_mods) {
        $t->{'fabric:load_conditions'} = { condition => 'fabric:all_mods_loaded', values => [$mod] }
            if $file =~ m{/track_\Q$mod\E_};
    }
    walk($t, sub {
        my $n = shift;
        $n->{predicate} = item_predicate($n->{predicate})
            if ($n->{condition} // '') eq 'minecraft:match_tool' && ref $n->{predicate} eq 'HASH';
    });
    return $t;
}

sub convert_advancement {
    my $a = shift;
    if (my $icon = $a->{display} && $a->{display}{icon}) {
        $icon->{id} = delete $icon->{item} if exists $icon->{item};
    }
    for my $c (values %{ $a->{criteria} // {} }) {
        next unless ($c->{trigger} // '') eq 'minecraft:inventory_changed' && $c->{conditions}{items};
        $c->{conditions}{items} = [ map { item_predicate($_) } @{ $c->{conditions}{items} } ];
    }
    return $a;
}

# --- internal tags -------------------------------------------------------------------------------------------------

my %tag_rename = (
    'c:string' => 'c:strings',
    'c:workbench' => 'c:player_workstations/crafting_tables',
    'c:colorless_glass' => 'c:glass_blocks/colorless',
);

sub rename_c_tag {
    my $t = shift;
    return $tag_rename{$t} if $tag_rename{$t};
    return "c:dyes/$1" if $t =~ /^c:(\w+)_dyes$/;
    return "c:$2/$1" if $t =~ /^c:(\w+)_(ingots|nuggets|plates)$/;
    return $t;
}

sub convert_internal_tag {
    my $t = shift;
    my @values;
    for my $v (@{ $t->{values} }) {
        my $id = ref $v ? $v->{id} : $v;
        next if $id =~ /^#forge:/; # no Forge tags on Fabric 26.1
        $id = '#' . rename_c_tag(substr $id, 1) if $id =~ /^#c:/;
        push @values, ref $v ? { %$v, id => $id } : $id;
    }
    $t->{values} = \@values;
    return $t;
}

# --- main ----------------------------------------------------------------------------------------------------------

# Optional arguments pick sections (recipe, loot_table, advancement, tags). Only loot_table, advancement and tags
# are safe to rerun on already converted files.
my %run = map { $_ => 1 } (@ARGV ? @ARGV : qw(recipe loot_table advancement tags));
my %count;
if ($run{recipe}) {
    for my $f (files_under("$root/recipe")) { write_json($f, convert_recipe(read_json($f))); $count{recipes}++; }
}
if ($run{loot_table}) {
    for my $f (files_under("$root/loot_table")) { write_json($f, convert_loot_table(read_json($f), $f)); $count{loot_tables}++; }
}
if ($run{advancement}) {
    for my $f (files_under("$root/advancement")) { write_json($f, convert_advancement(read_json($f))); $count{advancements}++; }
}
if ($run{tags}) {
    for my $f (files_under("$root/tags/item/internal")) { write_json($f, convert_internal_tag(read_json($f))); $count{tags}++; }
}
print "converted: " . join(', ', map { "$count{$_} $_" } sort keys %count) . "\n";
