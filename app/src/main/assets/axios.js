/* axios v0.5.4 | (c) 2015 by Matt Zabriskie */
var axios=function(e){function t(n){if(r[n])return r[n].exports;var o=r[n]={exports:{},id:n,loaded:!1};return e[n].call(o.exports,o,o.exports,t),o.loaded=!0,o.exports}var r={};return t.m=e,t.c=r,t.p="",t(0)}([function(e,t,r){e.exports=r(1)},function(e,t,r){"use strict";var n=r(3),o=r(4),i=r(5),s=r(6),u=r(7);!function(){var e=r(2);e&&"function"==typeof e.polyfill&&e.polyfill()}();var a=e.exports=function c(e){e=o.merge({method:"get",headers:{},transformRequest:n.transformRequest,transformResponse:n.transformResponse},e),e.withCredentials=e.withCredentials||n.withCredentials;var t=[s,void 0],r=Promise.resolve(e);for(c.interceptors.request.forEach(function(e){t.unshift(e.fulfilled,e.rejected)}),c.interceptors.response.forEach(function(e){t.push(e.fulfilled,e.rejected)});t.length;)r=r.then(t.shift(),t.shift());return r.success=function(e){return i("success","then","https://github.com/mzabriskie/axios/blob/master/README.md#response-api"),r.then(function(t){e(t.data,t.status,t.headers,t.config)}),r},r.error=function(e){return i("error","catch","https://github.com/mzabriskie/axios/blob/master/README.md#response-api"),r.then(null,function(t){e(t.data,t.status,t.headers,t.config)}),r},r};a.defaults=n,a.all=function(e){return Promise.all(e)},a.spread=r(8),a.interceptors={request:new u,response:new u},function(){function e(){o.forEach(arguments,function(e){a[e]=function(t,r){return a(o.merge(r||{},{method:e,url:t}))}})}function t(){o.forEach(arguments,function(e){a[e]=function(t,r,n){return a(o.merge(n||{},{method:e,url:t,data:r}))}})}e("delete","get","head"),t("post","put","patch")}()},function(e){e.exports={Promise:Promise}},function(e,t,r){"use strict";var n=r(4),o=/^\)\]\}',?\n/,i={"Content-Type":"application/x-www-form-urlencoded"};e.exports={transformRequest:[function(e,t){return n.isFormData(e)?e:n.isArrayBuffer(e)?e:n.isArrayBufferView(e)?e.buffer:!n.isObject(e)||n.isFile(e)||n.isBlob(e)?e:(!n.isUndefined(t)&&n.isUndefined(t["Content-Type"])&&(t["Content-Type"]="application/json;charset=utf-8"),JSON.stringify(e))}],transformResponse:[function(e){if("string"==typeof e){e=e.replace(o,"");try{e=JSON.parse(e)}catch(t){}}return e}],headers:{common:{Accept:"application/json, text/plain, */*"},patch:n.merge(i),post:n.merge(i),put:n.merge(i)},xsrfCookieName:"XSRF-TOKEN",xsrfHeaderName:"X-XSRF-TOKEN"}},function(e){"use strict";function t(e){return"[object Array]"===m.call(e)}function r(e){return"[object ArrayBuffer]"===m.call(e)}function n(e){return"[object FormData]"===m.call(e)}function o(e){return"undefined"!=typeof ArrayBuffer&&ArrayBuffer.isView?ArrayBuffer.isView(e):e&&e.buffer&&e.buffer instanceof ArrayBuffer}function i(e){return"string"==typeof e}function s(e){return"number"==typeof e}function u(e){return"undefined"==typeof e}function a(e){return null!==e&&"object"==typeof e}function c(e){return"[object Date]"===m.call(e)}function f(e){return"[object File]"===m.call(e)}function l(e){return"[object Blob]"===m.call(e)}function p(e){return e.replace(/^\s*/,"").replace(/\s*$/,"")}function h(e,r){if(null!==e&&"undefined"!=typeof e){var n=t(e)||"object"==typeof e&&!isNaN(e.length);if("object"==typeof e||n||(e=[e]),n)for(var o=0,i=e.length;i>o;o++)r.call(null,e[o],o,e);else for(var s in e)e.hasOwnProperty(s)&&r.call(null,e[s],s,e)}}function d(){var e={};return h(arguments,function(t){h(t,function(t,r){e[r]=t})}),e}var m=Object.prototype.toString;e.exports={isArray:t,isArrayBuffer:r,isFormData:n,isArrayBufferView:o,isString:i,isNumber:s,isObject:a,isUndefined:u,isDate:c,isFile:f,isBlob:l,forEach:h,merge:d,trim:p}},function(e){"use strict";e.exports=function(e,t,r){try{console.warn("DEPRECATED method `"+e+"`."+(t?" Use `"+t+"` instead.":"")+" This method will be removed in a future release."),r&&console.warn("For more information about usage see "+r)}catch(n){}}},function(e,t,r){(function(t){"use strict";e.exports=function(e){return new Promise(function(n,o){try{"undefined"!=typeof window?r(9)(n,o,e):"undefined"!=typeof t&&r(9)(n,o,e)}catch(i){o(i)}})}}).call(t,r(10))},function(e,t,r){"use strict";function n(){this.handlers=[]}var o=r(4);n.prototype.use=function(e,t){return this.handlers.push({fulfilled:e,rejected:t}),this.handlers.length-1},n.prototype.eject=function(e){this.handlers[e]&&(this.handlers[e]=null)},n.prototype.forEach=function(e){o.forEach(this.handlers,function(t){null!==t&&e(t)})},e.exports=n},function(e){"use strict";e.exports=function(e){return function(t){e.apply(null,t)}}},function(e,t,r){"use strict";var n=r(3),o=r(4),i=r(11),s=r(12),u=r(13),a=r(14),c=r(15);e.exports=function(e,t,r){var f=a(r.data,r.headers,r.transformRequest),l=o.merge(n.headers.common,n.headers[r.method]||{},r.headers||{});o.isFormData(f)&&delete l["Content-Type"];var p=new(XMLHttpRequest||ActiveXObject)("Microsoft.XMLHTTP");p.open(r.method.toUpperCase(),i(r.url,r.params),!0),p.onreadystatechange=function(){if(p&&4===p.readyState){var n=u(p.getAllResponseHeaders()),o=-1!==["text",""].indexOf(r.responseType||"")?p.responseText:p.response,i={data:a(o,n,r.transformResponse),status:p.status,statusText:p.statusText,headers:n,config:r};(p.status>=200&&p.status<300?e:t)(i),p=null}};var h=c(r.url)?s.read(r.xsrfCookieName||n.xsrfCookieName):void 0;if(h&&(l[r.xsrfHeaderName||n.xsrfHeaderName]=h),o.forEach(l,function(e,t){f||"content-type"!==t.toLowerCase()?p.setRequestHeader(t,e):delete l[t]}),r.withCredentials&&(p.withCredentials=!0),r.responseType)try{p.responseType=r.responseType}catch(d){if("json"!==p.responseType)throw d}o.isArrayBuffer(f)&&(f=new DataView(f)),p.send(f)}},function(e){function t(){if(!i){i=!0;for(var e,t=o.length;t;){e=o,o=[];for(var r=-1;++r<t;)e[r]();t=o.length}i=!1}}function r(){}var n=e.exports={},o=[],i=!1;n.nextTick=function(e){o.push(e),i||setTimeout(t,0)},n.title="browser",n.browser=!0,n.env={},n.argv=[],n.version="",n.versions={},n.on=r,n.addListener=r,n.once=r,n.off=r,n.removeListener=r,n.removeAllListeners=r,n.emit=r,n.binding=function(){throw new Error("process.binding is not supported")},n.cwd=function(){return"/"},n.chdir=function(){throw new Error("process.chdir is not supported")},n.umask=function(){return 0}},function(e,t,r){"use strict";function n(e){return encodeURIComponent(e).replace(/%40/gi,"@").replace(/%3A/gi,":").replace(/%24/g,"$").replace(/%2C/gi,",").replace(/%20/g,"+")}var o=r(4);e.exports=function(e,t){if(!t)return e;var r=[];return o.forEach(t,function(e,t){null!==e&&"undefined"!=typeof e&&(o.isArray(e)||(e=[e]),o.forEach(e,function(e){o.isDate(e)?e=e.toISOString():o.isObject(e)&&(e=JSON.stringify(e)),r.push(n(t)+"="+n(e))}))}),r.length>0&&(e+=(-1===e.indexOf("?")?"?":"&")+r.join("&")),e}},function(e,t,r){"use strict";var n=r(4);e.exports={write:function(e,t,r,o,i,s){var u=[];u.push(e+"="+encodeURIComponent(t)),n.isNumber(r)&&u.push("expires="+new Date(r).toGMTString()),n.isString(o)&&u.push("path="+o),n.isString(i)&&u.push("domain="+i),s===!0&&u.push("secure"),document.cookie=u.join("; ")},read:function(e){var t=document.cookie.match(new RegExp("(^|;\\s*)("+e+")=([^;]*)"));return t?decodeURIComponent(t[3]):null},remove:function(e){this.write(e,"",Date.now()-864e5)}}},function(e,t,r){"use strict";var n=r(4);e.exports=function(e){var t,r,o,i={};return e?(n.forEach(e.split("\n"),function(e){o=e.indexOf(":"),t=n.trim(e.substr(0,o)).toLowerCase(),r=n.trim(e.substr(o+1)),t&&(i[t]=i[t]?i[t]+", "+r:r)}),i):i}},function(e,t,r){"use strict";var n=r(4);e.exports=function(e,t,r){return n.forEach(r,function(r){e=r(e,t)}),e}},function(e,t,r){"use strict";function n(e){var t=e;return s&&(u.setAttribute("href",t),t=u.href),u.setAttribute("href",t),{href:u.href,protocol:u.protocol?u.protocol.replace(/:$/,""):"",host:u.host,search:u.search?u.search.replace(/^\?/,""):"",hash:u.hash?u.hash.replace(/^#/,""):"",hostname:u.hostname,port:u.port,pathname:"/"===u.pathname.charAt(0)?u.pathname:"/"+u.pathname}}var o,i=r(4),s=/(msie|trident)/i.test(navigator.userAgent),u=document.createElement("a");o=n(window.location.href),e.exports=function(e){var t=i.isString(e)?n(e):e;return t.protocol===o.protocol&&t.host===o.host}}]);
//# sourceMappingURL=axios.standalone.min.map
