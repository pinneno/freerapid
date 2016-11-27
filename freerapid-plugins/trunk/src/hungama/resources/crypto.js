//http://www.hungama.com/themes/hungamaTheme/mp/jquery.jqcrypt.pack.js
function base64_decode(a) {
    var b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var c, o2, o3, h1, h2, h3, h4, bits, i = 0,
        ac = 0,
        dec = "",
        tmp_arr = [];
    if (!a) {
        return a
    }
    a += '';
    do {
        h1 = b.indexOf(a.charAt(i++));
        h2 = b.indexOf(a.charAt(i++));
        h3 = b.indexOf(a.charAt(i++));
        h4 = b.indexOf(a.charAt(i++));
        bits = h1 << 18 | h2 << 12 | h3 << 6 | h4;
        c = bits >> 16 & 0xff;
        o2 = bits >> 8 & 0xff;
        o3 = bits & 0xff;
        if (h3 == 64) {
            tmp_arr[ac++] = String.fromCharCode(c)
        } else if (h4 == 64) {
            tmp_arr[ac++] = String.fromCharCode(c, o2)
        } else {
            tmp_arr[ac++] = String.fromCharCode(c, o2, o3)
        }
    } while (i < a.length);
    dec = tmp_arr.join('');
    dec = this.utf8_decode(dec);
    return dec
}
function base64_encode(a) {
    var b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var c, o2, o3, h1, h2, h3, h4, bits, i = 0,
        ac = 0,
        enc = "",
        tmp_arr = [];
    if (!a) {
        return a
    }
    a = this.utf8_encode(a + '');
    do {
        c = a.charCodeAt(i++);
        o2 = a.charCodeAt(i++);
        o3 = a.charCodeAt(i++);
        bits = c << 16 | o2 << 8 | o3;
        h1 = bits >> 18 & 0x3f;
        h2 = bits >> 12 & 0x3f;
        h3 = bits >> 6 & 0x3f;
        h4 = bits & 0x3f;
        tmp_arr[ac++] = b.charAt(h1) + b.charAt(h2) + b.charAt(h3) + b.charAt(h4)
    } while (i < a.length);
    enc = tmp_arr.join('');
    switch (a.length % 3) {
    case 1:
        enc = enc.slice(0, - 2) + '==';
        break;
    case 2:
        enc = enc.slice(0, - 1) + '=';
        break
    }
    return enc
}
function chr(a) {
    if (a > 0xFFFF) {
        a -= 0x10000;
        return String.fromCharCode(0xD800 + (a >> 10), 0xDC00 + (a & 0x3FF))
    } else {
        return String.fromCharCode(a)
    }
}
function implode(a, b) {
    var i = '',
        retVal = '',
        tGlue = '';
    if (arguments.length === 1) {
        b = a;
        a = ''
    }
    if (typeof (b) === 'object') {
        if (b instanceof Array) {
            return b.join(a)
        } else {
            for (i in b) {
                retVal += tGlue + b[i];
                tGlue = a
            }
            return retVal
        }
    } else {
        return b
    }
}
function join(a, b) {
    return this.implode(a, b)
}
function ord(a) {
    var b = a + '';
    var c = b.charCodeAt(0);
    if (0xD800 <= c && c <= 0xDBFF) {
        var d = c;
        if (b.length === 1) {
            return c
        }
        var e = b.charCodeAt(1);
        if (!e) {}
        return ((d - 0xD800) * 0x400) + (e - 0xDC00) + 0x10000
    }
    if (0xDC00 <= c && c <= 0xDFFF) {
        return c
    }
    return c
}
function str_pad(c, d, e, f) {
    var g = '',
        pad_to_go;
    var h = function (s, a) {
        var b = '',
            i;
        while (b.length < a) {
            b += s
        }
        b = b.substr(0, a);
        return b
    };
    c += '';
    e = e !== undefined ? e : ' ';
    if (f != 'STR_PAD_LEFT' && f != 'STR_PAD_RIGHT' && f != 'STR_PAD_BOTH') {
        f = 'STR_PAD_RIGHT'
    }
    if ((pad_to_go = d - c.length) > 0) {
        if (f == 'STR_PAD_LEFT') {
            c = h(e, pad_to_go) + c
        } else if (f == 'STR_PAD_RIGHT') {
            c = c + h(e, pad_to_go)
        } else if (f == 'STR_PAD_BOTH') {
            g = h(e, Math.ceil(pad_to_go / 2));
            c = g + c + g;
            c = c.substr(0, d)
        }
    }
    return c
}
function str_split(a, b) {
    if (a === undefined || !a.toString || b < 1) {
        return false
    }
    return a.toString().match(new RegExp('.{1,' + (b || '1') + '}', 'g'))
}
function strlen(d) {
    var e = d + '';
    var i = 0,
        chr = '',
        lgth = 0;
    var f = function (a, i) {
        var b = a.charCodeAt(i);
        var c = '',
            prev = '';
        if (0xD800 <= b && b <= 0xDBFF) {
            if (a.length <= (i + 1)) {
                throw 'High surrogate without following low surrogate';
            }
            c = a.charCodeAt(i + 1);
            if (0xDC00 > c || c > 0xDFFF) {
                throw 'High surrogate without following low surrogate';
            }
            return a.charAt(i) + a.charAt(i + 1)
        } else if (0xDC00 <= b && b <= 0xDFFF) {
            if (i === 0) {
                throw 'Low surrogate without preceding high surrogate';
            }
            prev = a.charCodeAt(i - 1);
            if (0xD800 > prev || prev > 0xDBFF) {
                throw 'Low surrogate without preceding high surrogate';
            }
            return false
        }
        return a.charAt(i)
    };
    for (i = 0, lgth = 0; i < e.length; i++) {
        if ((chr = f(e, i)) === false) {
            continue
        }
        lgth++
    }
    return lgth
}
function utf8_decode(a) {
    var b = [],
        i = 0,
        ac = 0,
        c1 = 0,
        c2 = 0,
        c3 = 0;
    a += '';
    while (i < a.length) {
        c1 = a.charCodeAt(i);
        if (c1 < 128) {
            b[ac++] = String.fromCharCode(c1);
            i++
        } else if ((c1 > 191) && (c1 < 224)) {
            c2 = a.charCodeAt(i + 1);
            b[ac++] = String.fromCharCode(((c1 & 31) << 6) | (c2 & 63));
            i += 2
        } else {
            c2 = a.charCodeAt(i + 1);
            c3 = a.charCodeAt(i + 2);
            b[ac++] = String.fromCharCode(((c1 & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
            i += 3
        }
    }
    return b.join('')
}
function utf8_encode(a) {
    var b = (a + '');
    var c = "";
    var d, end;
    var e = 0;
    d = end = 0;
    e = b.length;
    for (var n = 0; n < e; n++) {
        var f = b.charCodeAt(n);
        var g = null;
        if (f < 128) {
            end++
        } else if (f > 127 && f < 2048) {
            g = String.fromCharCode((f >> 6) | 192) + String.fromCharCode((f & 63) | 128)
        } else {
            g = String.fromCharCode((f >> 12) | 224) + String.fromCharCode(((f >> 6) & 63) | 128) + String.fromCharCode((f & 63) | 128)
        }
        if (g !== null) {
            if (end > d) {
                c += b.substring(d, end)
            }
            c += g;
            d = end = n + 1
        }
    }
    if (end > d) {
        c += b.substring(d, b.length)
    }
    return c
}
function c2sencrypt(s, k) {
    k = str_split(str_pad('', strlen(s), k));
    sa = str_split(s);
    for (var i in sa) {
        t = ord(sa[i]) + ord(k[i]);
        sa[i] = chr(t > 255 ? (t - 256) : t)
    }
    return escape(join('', sa))
}
function randomString() {
    var a = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz!@#$%^&*()";
    var b = 8;
    var c = '';
    for (var i = 0; i < b; i++) {
        var d = Math.floor(Math.random() * a.length);
        c += a.substring(d, d + 1)
    }
    return c
}