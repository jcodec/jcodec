set -xue

cat <<EOF
var fs = require('fs');

jcodec = {};
jslang = {};
jsutil = {};
jslang.reflect = {};

EOF

cat target/classes/stjs.js

echo

cat <<EOF2
stjs.mainCallDisabled = true;

Boolean.parseBoolean = function(s) {
    return ((s != null) && s.equalsIgnoreCase("true"));
}
Integer.toHexString = function(i) {
    return (i).toString(16);
}
Character = Number;

String.format = function(_args) {
	var args = Array.prototype.slice.call(arguments);
	return args.join(", ")
}
EOF2

cat ../jslang/target/classes/jslang.js
cat target/classes/jcodec.js
cat target/generated-test-js/jcodec.js

cat <<EOF3
var f = new File("hello.txt")

console.log(f);
console.log(System.currentTimeMillis());

var start = System.currentTimeMillis();
var conf = new PerformanceTest();
console.log(conf);
conf.testNoContainer();
var time = System.currentTimeMillis() - start;
console.log("ConformanceTest.testNoContainer ok "+time+" msec");

process.exit(0)
EOF3
