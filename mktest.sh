cat <<EOF
var fs = require('fs');

jcodec = {};
jslang = {};
jsutil = {};
jslang.reflect = {};

EOF

cat /Users/zhukov/workspaces/jcodecjs/jslang/target/classes/stjs.js

echo

cat <<EOF2
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

cat /Users/zhukov/workspaces/jcodecjs/jslang/target/classes/jslang.js
cat /Users/zhukov/git/jcodec/target/classes/jcodec.js
cat /Users/zhukov/git/jcodec/target/generated-test-js/jcodec.js

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
