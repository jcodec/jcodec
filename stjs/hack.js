const fs = require('fs');

const javasrc = "src";
const stjssrc = "stjs";

String.prototype.equals = function (s) { return s == this; };
String.prototype.contains = function (it) { return this.indexOf(it) >= 0; };
String.prototype.matches = function (regexp) { return this.match("^" + regexp + "$") != null; }
String.prototype.replaceAll = function (regexp, replace) { return this.replace(new RegExp(regexp, "g"), replace); }

hackSource(javasrc);
fs.copyFileSync(stjssrc + '/Platform.java', javasrc + '/main/java/org/jcodec/platform/Platform.java');
fs.copyFileSync(stjssrc + '/BaseInputStream.java', javasrc + '/main/java/org/jcodec/platform/BaseInputStream.java');
fs.copyFileSync(stjssrc + '/BaseOutputStream.java', javasrc + '/main/java/org/jcodec/platform/BaseOutputStream.java');

fs.copyFileSync(stjssrc + '/Logger.java', javasrc + '/main/java/org/jcodec/common/logging/Logger.java');
fs.copyFileSync(stjssrc + '/Preconditions.java', javasrc + '/main/java/org/jcodec/common/Preconditions.java');

fs.copyFileSync(stjssrc + '/ToJSON.java', javasrc + '/main/java/org/jcodec/common/tools/ToJSON.java');

function find(file) {
    var result = [];
    var stack = [];
    stack.push(file);
    while (0 != stack.length) {
        var f = stack.pop();
        result.push(f);
        const stat = fs.statSync(f);

        if (stat && stat.isDirectory()) {
            fs.readdirSync(f).map(x => f + "/" + x).forEach(p => {
                stack.push(p);
            });
        }
    }
    return result;
}

function hackSource(srcDir) {
    find(srcDir).filter(f => f.endsWith(".java")).forEach(x => {
        var lines = fs.readFileSync(x, { encoding: "utf8" }).split("\n");
        var collect = lines.map(line => {
            if (line.equals("import java.util.Iterator;") || line.contains("java.lang.Math")
                || line.contains("java.lang.Integer") || line.matches("^import.*java.lang.String[\\.;].*")) {
                return line;
            }
            return line
                .replaceAll("^import java\\.", "import js.") //
                .replaceAll("^import javax\\.", "import jsx.") //
                .replaceAll("^import static java\\.", "import static js.")
                .replaceAll("^import static javax\\.", "import static jsx.") //
                .replaceAll("synchronized \\(.*\\) ", "")
                .replaceAll("synchronized ", "");
        });
        var pkgLine = collect.filter(line => line.matches("package .*;"));
        if (pkgLine.length > 0) {
            var pkg = pkgLine[0];
            var indexOf = collect.indexOf(pkg);
            collect = collect.slice(0, indexOf + 1)
                .concat([
                    "import js.lang.IllegalArgumentException;",
                    "import js.lang.IllegalStateException;",
                    "import js.lang.Comparable;",
                    "import js.lang.StringBuilder;",
                    "import js.lang.System;",
                    "import js.lang.Runtime;",
                    "import js.lang.Runnable;",
                    "import js.lang.Process;",
                    "import js.lang.ThreadLocal;",
                    "import js.lang.IndexOutOfBoundsException;",
                    "import js.lang.Thread;",
                    "import js.lang.NullPointerException;"
                ])
                .concat(collect.slice(indexOf + 1));
        }
        ;
        console.log(x);
        fs.writeFileSync(x, collect.join("\n"));
    });
}
