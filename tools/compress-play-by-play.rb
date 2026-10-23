# Collapses the per-minute damage lines of a play-by-play (see CLAUDE.md)
# into one damage summary per time skip. Usage: ruby compress-play-by-play.rb FILE
taken = Hash.new(0); dealt = Hash.new(0)
flush = lambda do |out|
  out << "              damage taken: " + taken.map { |k, v| "#{k} #{v}" }.join(", ") + " MJ" unless taken.empty?
  out << "              damage dealt: " + dealt.map { |k, v| "#{k} #{v}" }.join(", ") + " MJ" unless dealt.empty?
  taken.clear; dealt.clear
end
out = []
ARGF.each_line do |l|
  l = l.chomp
  if l =~ /DAMAGE_TAKEN\s+Hit by (.+?): (<?\d+) damage/
    taken[$1] += $2.delete("<").to_i
  elsif l =~ /DAMAGE_DEALT\s+(.+?) hit by (\w+): (<?\d+) damage/
    dealt["#{$1} (#{$2})"] += $3.delete("<").to_i
  else
    flush.call(out) if l.include?("-- skip ran")
    out << l
  end
end
flush.call(out)
puts out
