set -xue

cat <<EOF
var fs = require('fs');

jcodec = {};
jslang = {};
jsutil = {};
jslang.reflect = {};

EOF

cat target/classes/stjs.js | sed 's/charAt/charCodeAt/g'

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

cat ../jslang/target/classes/jslang.js | sed 's/charAt/charCodeAt/g'
cat target/classes/jcodec.js | sed 's/charAt/charCodeAt/g'
cat target/generated-test-js/jcodec.js | sed 's/charAt/charCodeAt/g'

tests=`find src/test/ -name '*Test.java' | sed -e 's}.*/}}' -e 's/.java$//' | tr '\n' ',' | sed 's/,$//'`
cat <<EOF3

var testclasses=[$tests];
testclasses.forEach(cls => {
    var t = new cls();
    for(var m in t) {
        if (m.startsWith("test")) {
            console.log(t, m);
            t[m]();
        }
    }
});
process.exit(0);
EOF3
