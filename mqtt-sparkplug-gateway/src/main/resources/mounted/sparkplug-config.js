(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("/res/sys/js/react.js"), require("/res/sys/js/react-dom.js"));
	else if(typeof define === 'function' && define.amd)
		define("com.inductiveautomation.mqtt.sparkplugb.gateway", ["/res/sys/js/react.js", "/res/sys/js/react-dom.js"], factory);
	else if(typeof exports === 'object')
		exports["com.inductiveautomation.mqtt.sparkplugb.gateway"] = factory(require("/res/sys/js/react.js"), require("/res/sys/js/react-dom.js"));
	else
		root["com.inductiveautomation.mqtt.sparkplugb.gateway"] = factory(root["/res/sys/js/react.js"], root["/res/sys/js/react-dom.js"]);
})(this, (__WEBPACK_EXTERNAL_MODULE__704__, __WEBPACK_EXTERNAL_MODULE__739__) => {
return /******/ (() => { // webpackBootstrap
/******/ 	"use strict";
/******/ 	var __webpack_modules__ = ({

/***/ 56
(module, __unused_webpack_exports, __webpack_require__) {



/* istanbul ignore next  */
function setAttributesWithoutAttributes(styleElement) {
  var nonce =  true ? __webpack_require__.nc : 0;
  if (nonce) {
    styleElement.setAttribute("nonce", nonce);
  }
}
module.exports = setAttributesWithoutAttributes;

/***/ },

/***/ 72
(module) {



var stylesInDOM = [];
function getIndexByIdentifier(identifier) {
  var result = -1;
  for (var i = 0; i < stylesInDOM.length; i++) {
    if (stylesInDOM[i].identifier === identifier) {
      result = i;
      break;
    }
  }
  return result;
}
function modulesToDom(list, options) {
  var idCountMap = {};
  var identifiers = [];
  for (var i = 0; i < list.length; i++) {
    var item = list[i];
    var id = options.base ? item[0] + options.base : item[0];
    var count = idCountMap[id] || 0;
    var identifier = "".concat(id, " ").concat(count);
    idCountMap[id] = count + 1;
    var indexByIdentifier = getIndexByIdentifier(identifier);
    var obj = {
      css: item[1],
      media: item[2],
      sourceMap: item[3],
      supports: item[4],
      layer: item[5]
    };
    if (indexByIdentifier !== -1) {
      stylesInDOM[indexByIdentifier].references++;
      stylesInDOM[indexByIdentifier].updater(obj);
    } else {
      var updater = addElementStyle(obj, options);
      options.byIndex = i;
      stylesInDOM.splice(i, 0, {
        identifier: identifier,
        updater: updater,
        references: 1
      });
    }
    identifiers.push(identifier);
  }
  return identifiers;
}
function addElementStyle(obj, options) {
  var api = options.domAPI(options);
  api.update(obj);
  var updater = function updater(newObj) {
    if (newObj) {
      if (newObj.css === obj.css && newObj.media === obj.media && newObj.sourceMap === obj.sourceMap && newObj.supports === obj.supports && newObj.layer === obj.layer) {
        return;
      }
      api.update(obj = newObj);
    } else {
      api.remove();
    }
  };
  return updater;
}
module.exports = function (list, options) {
  options = options || {};
  list = list || [];
  var lastIdentifiers = modulesToDom(list, options);
  return function update(newList) {
    newList = newList || [];
    for (var i = 0; i < lastIdentifiers.length; i++) {
      var identifier = lastIdentifiers[i];
      var index = getIndexByIdentifier(identifier);
      stylesInDOM[index].references--;
    }
    var newLastIdentifiers = modulesToDom(newList, options);
    for (var _i = 0; _i < lastIdentifiers.length; _i++) {
      var _identifier = lastIdentifiers[_i];
      var _index = getIndexByIdentifier(_identifier);
      if (stylesInDOM[_index].references === 0) {
        stylesInDOM[_index].updater();
        stylesInDOM.splice(_index, 1);
      }
    }
    lastIdentifiers = newLastIdentifiers;
  };
};

/***/ },

/***/ 113
(module) {



/* istanbul ignore next  */
function styleTagTransform(css, styleElement) {
  if (styleElement.styleSheet) {
    styleElement.styleSheet.cssText = css;
  } else {
    while (styleElement.firstChild) {
      styleElement.removeChild(styleElement.firstChild);
    }
    styleElement.appendChild(document.createTextNode(css));
  }
}
module.exports = styleTagTransform;

/***/ },

/***/ 314
(module) {



/*
  MIT License http://www.opensource.org/licenses/mit-license.php
  Author Tobias Koppers @sokra
*/
module.exports = function (cssWithMappingToString) {
  var list = [];

  // return the list of modules as css string
  list.toString = function toString() {
    return this.map(function (item) {
      var content = "";
      var needLayer = typeof item[5] !== "undefined";
      if (item[4]) {
        content += "@supports (".concat(item[4], ") {");
      }
      if (item[2]) {
        content += "@media ".concat(item[2], " {");
      }
      if (needLayer) {
        content += "@layer".concat(item[5].length > 0 ? " ".concat(item[5]) : "", " {");
      }
      content += cssWithMappingToString(item);
      if (needLayer) {
        content += "}";
      }
      if (item[2]) {
        content += "}";
      }
      if (item[4]) {
        content += "}";
      }
      return content;
    }).join("");
  };

  // import a list of modules into the list
  list.i = function i(modules, media, dedupe, supports, layer) {
    if (typeof modules === "string") {
      modules = [[null, modules, undefined]];
    }
    var alreadyImportedModules = {};
    if (dedupe) {
      for (var k = 0; k < this.length; k++) {
        var id = this[k][0];
        if (id != null) {
          alreadyImportedModules[id] = true;
        }
      }
    }
    for (var _k = 0; _k < modules.length; _k++) {
      var item = [].concat(modules[_k]);
      if (dedupe && alreadyImportedModules[item[0]]) {
        continue;
      }
      if (typeof layer !== "undefined") {
        if (typeof item[5] === "undefined") {
          item[5] = layer;
        } else {
          item[1] = "@layer".concat(item[5].length > 0 ? " ".concat(item[5]) : "", " {").concat(item[1], "}");
          item[5] = layer;
        }
      }
      if (media) {
        if (!item[2]) {
          item[2] = media;
        } else {
          item[1] = "@media ".concat(item[2], " {").concat(item[1], "}");
          item[2] = media;
        }
      }
      if (supports) {
        if (!item[4]) {
          item[4] = "".concat(supports);
        } else {
          item[1] = "@supports (".concat(item[4], ") {").concat(item[1], "}");
          item[4] = supports;
        }
      }
      list.push(item);
    }
  };
  return list;
};

/***/ },

/***/ 354
(module) {



module.exports = function (item) {
  var content = item[1];
  var cssMapping = item[3];
  if (!cssMapping) {
    return content;
  }
  if (typeof btoa === "function") {
    var base64 = btoa(unescape(encodeURIComponent(JSON.stringify(cssMapping))));
    var data = "sourceMappingURL=data:application/json;charset=utf-8;base64,".concat(base64);
    var sourceMapping = "/*# ".concat(data, " */");
    return [content].concat([sourceMapping]).join("\n");
  }
  return [content].join("\n");
};

/***/ },

/***/ 365
(module, __webpack_exports__, __webpack_require__) {

/* harmony export */ __webpack_require__.d(__webpack_exports__, {
/* harmony export */   A: () => (__WEBPACK_DEFAULT_EXPORT__)
/* harmony export */ });
/* harmony import */ var _node_modules_css_loader_dist_runtime_sourceMaps_js__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(354);
/* harmony import */ var _node_modules_css_loader_dist_runtime_sourceMaps_js__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(_node_modules_css_loader_dist_runtime_sourceMaps_js__WEBPACK_IMPORTED_MODULE_0__);
/* harmony import */ var _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(314);
/* harmony import */ var _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_1___default = /*#__PURE__*/__webpack_require__.n(_node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_1__);
// Imports


var ___CSS_LOADER_EXPORT___ = _node_modules_css_loader_dist_runtime_api_js__WEBPACK_IMPORTED_MODULE_1___default()((_node_modules_css_loader_dist_runtime_sourceMaps_js__WEBPACK_IMPORTED_MODULE_0___default()));
// Module
___CSS_LOADER_EXPORT___.push([module.id, `.mqtt-config-page {
    font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    padding: 20px;
    max-width: 1280px;
    margin: 0 auto;
    color: #1f2933;
}

.page-header {
    margin-bottom: 20px;
}

.page-header h1 {
    margin: 0 0 6px 0;
    font-size: 24px;
}

.page-description {
    margin: 0;
    color: #4f5d6b;
}

.tabs {
    display: flex;
    gap: 8px;
    border-bottom: 1px solid #d9e2ec;
    margin-bottom: 16px;
}

.tab {
    background: none;
    border: none;
    padding: 10px 14px;
    cursor: pointer;
    color: #52606d;
    border-bottom: 2px solid transparent;
    font-weight: 600;
}

.tab.active {
    color: #102a43;
    border-bottom-color: #0b76b7;
}

.tab-content {
    background: #fff;
    border: 1px solid #d9e2ec;
    border-radius: 8px;
    padding: 18px;
}

.form-group {
    margin-bottom: 14px;
}

.form-group label {
    display: block;
    margin-bottom: 6px;
    font-weight: 600;
}

.form-group input,
.form-group select {
    width: 100%;
    padding: 8px 10px;
    border-radius: 6px;
    border: 1px solid #cbd2d9;
}

.form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
}

.mapping-list {
    display: grid;
    gap: 10px;
}

.mapping-item {
    border: 1px solid #e4e7eb;
    border-radius: 6px;
    padding: 10px;
    display: grid;
    gap: 8px;
    background: #f8f9fb;
}

.mapping-item.disabled {
    opacity: 0.65;
}

.mapping-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    color: #52606d;
    font-size: 14px;
}

.mapping-toolbar-actions {
    display: flex;
    gap: 8px;
}

.mapping-add {
    padding: 12px;
    border: 1px dashed #cbd2d9;
    border-radius: 8px;
    margin-bottom: 12px;
    background: #fff;
}

.mapping-add-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 12px;
    align-items: end;
}

.mapping-add-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.mapping-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 8px 10px;
    background: #fff;
    border: 1px solid #e4e7eb;
    border-radius: 6px;
}

.mapping-summary-text {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 600;
    color: #243b53;
    overflow: hidden;
}

.mapping-summary-label {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 360px;
}

.mapping-summary-arrow {
    color: #829ab1;
}

.mapping-summary-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.mapping-toggle {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: #1f2933;
}

.mapping-toggle input {
    width: 18px;
    height: 18px;
}

.field-warning {
    display: inline-block;
    margin-top: 4px;
    color: #b23b3b;
    font-size: 12px;
}

.validation-errors {
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 6px;
    background: #fff4e5;
    color: #7a4e00;
}

.validation-errors ul {
    margin: 8px 0 0 18px;
}

.mapping-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
}

.btn-primary {
    background: #0b76b7;
    color: #fff;
    border: none;
    padding: 8px 14px;
    border-radius: 6px;
    cursor: pointer;
}

.btn-secondary {
    background: #e4e7eb;
    color: #1f2933;
    border: none;
    padding: 8px 14px;
    border-radius: 6px;
    cursor: pointer;
}

.btn-danger {
    background: #d64545;
    color: #fff;
    border: none;
    padding: 8px 12px;
    border-radius: 6px;
    cursor: pointer;
}

.message {
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 6px;
}

.message.success {
    background: #e6fffa;
    color: #285e61;
}

.message.error {
    background: #ffe3e3;
    color: #8a1c1c;
}
`, "",{"version":3,"sources":["webpack://./src/styles.css"],"names":[],"mappings":"AAAA;IACI,4DAA4D;IAC5D,aAAa;IACb,iBAAiB;IACjB,cAAc;IACd,cAAc;AAClB;;AAEA;IACI,mBAAmB;AACvB;;AAEA;IACI,iBAAiB;IACjB,eAAe;AACnB;;AAEA;IACI,SAAS;IACT,cAAc;AAClB;;AAEA;IACI,aAAa;IACb,QAAQ;IACR,gCAAgC;IAChC,mBAAmB;AACvB;;AAEA;IACI,gBAAgB;IAChB,YAAY;IACZ,kBAAkB;IAClB,eAAe;IACf,cAAc;IACd,oCAAoC;IACpC,gBAAgB;AACpB;;AAEA;IACI,cAAc;IACd,4BAA4B;AAChC;;AAEA;IACI,gBAAgB;IAChB,yBAAyB;IACzB,kBAAkB;IAClB,aAAa;AACjB;;AAEA;IACI,mBAAmB;AACvB;;AAEA;IACI,cAAc;IACd,kBAAkB;IAClB,gBAAgB;AACpB;;AAEA;;IAEI,WAAW;IACX,iBAAiB;IACjB,kBAAkB;IAClB,yBAAyB;AAC7B;;AAEA;IACI,aAAa;IACb,2DAA2D;IAC3D,SAAS;AACb;;AAEA;IACI,aAAa;IACb,SAAS;AACb;;AAEA;IACI,yBAAyB;IACzB,kBAAkB;IAClB,aAAa;IACb,aAAa;IACb,QAAQ;IACR,mBAAmB;AACvB;;AAEA;IACI,aAAa;AACjB;;AAEA;IACI,aAAa;IACb,mBAAmB;IACnB,8BAA8B;IAC9B,mBAAmB;IACnB,cAAc;IACd,eAAe;AACnB;;AAEA;IACI,aAAa;IACb,QAAQ;AACZ;;AAEA;IACI,aAAa;IACb,0BAA0B;IAC1B,kBAAkB;IAClB,mBAAmB;IACnB,gBAAgB;AACpB;;AAEA;IACI,aAAa;IACb,2DAA2D;IAC3D,SAAS;IACT,gBAAgB;AACpB;;AAEA;IACI,aAAa;IACb,sBAAsB;IACtB,QAAQ;AACZ;;AAEA;IACI,aAAa;IACb,mBAAmB;IACnB,8BAA8B;IAC9B,SAAS;IACT,iBAAiB;IACjB,gBAAgB;IAChB,yBAAyB;IACzB,kBAAkB;AACtB;;AAEA;IACI,aAAa;IACb,mBAAmB;IACnB,SAAS;IACT,gBAAgB;IAChB,cAAc;IACd,gBAAgB;AACpB;;AAEA;IACI,mBAAmB;IACnB,gBAAgB;IAChB,uBAAuB;IACvB,gBAAgB;AACpB;;AAEA;IACI,cAAc;AAClB;;AAEA;IACI,aAAa;IACb,mBAAmB;IACnB,SAAS;AACb;;AAEA;IACI,aAAa;IACb,mBAAmB;IACnB,QAAQ;IACR,gBAAgB;IAChB,cAAc;AAClB;;AAEA;IACI,WAAW;IACX,YAAY;AAChB;;AAEA;IACI,qBAAqB;IACrB,eAAe;IACf,cAAc;IACd,eAAe;AACnB;;AAEA;IACI,mBAAmB;IACnB,kBAAkB;IAClB,kBAAkB;IAClB,mBAAmB;IACnB,cAAc;AAClB;;AAEA;IACI,oBAAoB;AACxB;;AAEA;IACI,aAAa;IACb,yBAAyB;IACzB,QAAQ;AACZ;;AAEA;IACI,mBAAmB;IACnB,WAAW;IACX,YAAY;IACZ,iBAAiB;IACjB,kBAAkB;IAClB,eAAe;AACnB;;AAEA;IACI,mBAAmB;IACnB,cAAc;IACd,YAAY;IACZ,iBAAiB;IACjB,kBAAkB;IAClB,eAAe;AACnB;;AAEA;IACI,mBAAmB;IACnB,WAAW;IACX,YAAY;IACZ,iBAAiB;IACjB,kBAAkB;IAClB,eAAe;AACnB;;AAEA;IACI,mBAAmB;IACnB,kBAAkB;IAClB,kBAAkB;AACtB;;AAEA;IACI,mBAAmB;IACnB,cAAc;AAClB;;AAEA;IACI,mBAAmB;IACnB,cAAc;AAClB","sourcesContent":[".mqtt-config-page {\n    font-family: \"Segoe UI\", \"Helvetica Neue\", Arial, sans-serif;\n    padding: 20px;\n    max-width: 1280px;\n    margin: 0 auto;\n    color: #1f2933;\n}\n\n.page-header {\n    margin-bottom: 20px;\n}\n\n.page-header h1 {\n    margin: 0 0 6px 0;\n    font-size: 24px;\n}\n\n.page-description {\n    margin: 0;\n    color: #4f5d6b;\n}\n\n.tabs {\n    display: flex;\n    gap: 8px;\n    border-bottom: 1px solid #d9e2ec;\n    margin-bottom: 16px;\n}\n\n.tab {\n    background: none;\n    border: none;\n    padding: 10px 14px;\n    cursor: pointer;\n    color: #52606d;\n    border-bottom: 2px solid transparent;\n    font-weight: 600;\n}\n\n.tab.active {\n    color: #102a43;\n    border-bottom-color: #0b76b7;\n}\n\n.tab-content {\n    background: #fff;\n    border: 1px solid #d9e2ec;\n    border-radius: 8px;\n    padding: 18px;\n}\n\n.form-group {\n    margin-bottom: 14px;\n}\n\n.form-group label {\n    display: block;\n    margin-bottom: 6px;\n    font-weight: 600;\n}\n\n.form-group input,\n.form-group select {\n    width: 100%;\n    padding: 8px 10px;\n    border-radius: 6px;\n    border: 1px solid #cbd2d9;\n}\n\n.form-row {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n    gap: 12px;\n}\n\n.mapping-list {\n    display: grid;\n    gap: 10px;\n}\n\n.mapping-item {\n    border: 1px solid #e4e7eb;\n    border-radius: 6px;\n    padding: 10px;\n    display: grid;\n    gap: 8px;\n    background: #f8f9fb;\n}\n\n.mapping-item.disabled {\n    opacity: 0.65;\n}\n\n.mapping-toolbar {\n    display: flex;\n    align-items: center;\n    justify-content: space-between;\n    margin-bottom: 10px;\n    color: #52606d;\n    font-size: 14px;\n}\n\n.mapping-toolbar-actions {\n    display: flex;\n    gap: 8px;\n}\n\n.mapping-add {\n    padding: 12px;\n    border: 1px dashed #cbd2d9;\n    border-radius: 8px;\n    margin-bottom: 12px;\n    background: #fff;\n}\n\n.mapping-add-row {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));\n    gap: 12px;\n    align-items: end;\n}\n\n.mapping-add-actions {\n    display: flex;\n    flex-direction: column;\n    gap: 8px;\n}\n\n.mapping-summary {\n    display: flex;\n    align-items: center;\n    justify-content: space-between;\n    gap: 12px;\n    padding: 8px 10px;\n    background: #fff;\n    border: 1px solid #e4e7eb;\n    border-radius: 6px;\n}\n\n.mapping-summary-text {\n    display: flex;\n    align-items: center;\n    gap: 10px;\n    font-weight: 600;\n    color: #243b53;\n    overflow: hidden;\n}\n\n.mapping-summary-label {\n    white-space: nowrap;\n    overflow: hidden;\n    text-overflow: ellipsis;\n    max-width: 360px;\n}\n\n.mapping-summary-arrow {\n    color: #829ab1;\n}\n\n.mapping-summary-actions {\n    display: flex;\n    align-items: center;\n    gap: 10px;\n}\n\n.mapping-toggle {\n    display: flex;\n    align-items: center;\n    gap: 6px;\n    font-weight: 600;\n    color: #1f2933;\n}\n\n.mapping-toggle input {\n    width: 18px;\n    height: 18px;\n}\n\n.field-warning {\n    display: inline-block;\n    margin-top: 4px;\n    color: #b23b3b;\n    font-size: 12px;\n}\n\n.validation-errors {\n    margin-bottom: 12px;\n    padding: 10px 12px;\n    border-radius: 6px;\n    background: #fff4e5;\n    color: #7a4e00;\n}\n\n.validation-errors ul {\n    margin: 8px 0 0 18px;\n}\n\n.mapping-actions {\n    display: flex;\n    justify-content: flex-end;\n    gap: 8px;\n}\n\n.btn-primary {\n    background: #0b76b7;\n    color: #fff;\n    border: none;\n    padding: 8px 14px;\n    border-radius: 6px;\n    cursor: pointer;\n}\n\n.btn-secondary {\n    background: #e4e7eb;\n    color: #1f2933;\n    border: none;\n    padding: 8px 14px;\n    border-radius: 6px;\n    cursor: pointer;\n}\n\n.btn-danger {\n    background: #d64545;\n    color: #fff;\n    border: none;\n    padding: 8px 12px;\n    border-radius: 6px;\n    cursor: pointer;\n}\n\n.message {\n    margin-bottom: 12px;\n    padding: 10px 12px;\n    border-radius: 6px;\n}\n\n.message.success {\n    background: #e6fffa;\n    color: #285e61;\n}\n\n.message.error {\n    background: #ffe3e3;\n    color: #8a1c1c;\n}\n"],"sourceRoot":""}]);
// Exports
/* harmony default export */ const __WEBPACK_DEFAULT_EXPORT__ = (___CSS_LOADER_EXPORT___);


/***/ },

/***/ 540
(module) {



/* istanbul ignore next  */
function insertStyleElement(options) {
  var element = document.createElement("style");
  options.setAttributes(element, options.attributes);
  options.insert(element, options.options);
  return element;
}
module.exports = insertStyleElement;

/***/ },

/***/ 659
(module) {



var memo = {};

/* istanbul ignore next  */
function getTarget(target) {
  if (typeof memo[target] === "undefined") {
    var styleTarget = document.querySelector(target);

    // Special case to return head of iframe instead of iframe itself
    if (window.HTMLIFrameElement && styleTarget instanceof window.HTMLIFrameElement) {
      try {
        // This will throw an exception if access to iframe is blocked
        // due to cross-origin restrictions
        styleTarget = styleTarget.contentDocument.head;
      } catch (e) {
        // istanbul ignore next
        styleTarget = null;
      }
    }
    memo[target] = styleTarget;
  }
  return memo[target];
}

/* istanbul ignore next  */
function insertBySelector(insert, style) {
  var target = getTarget(insert);
  if (!target) {
    throw new Error("Couldn't find a style target. This probably means that the value for the 'insert' parameter is invalid.");
  }
  target.appendChild(style);
}
module.exports = insertBySelector;

/***/ },

/***/ 704
(module) {

module.exports = __WEBPACK_EXTERNAL_MODULE__704__;

/***/ },

/***/ 739
(module) {

module.exports = __WEBPACK_EXTERNAL_MODULE__739__;

/***/ },

/***/ 825
(module) {



/* istanbul ignore next  */
function apply(styleElement, options, obj) {
  var css = "";
  if (obj.supports) {
    css += "@supports (".concat(obj.supports, ") {");
  }
  if (obj.media) {
    css += "@media ".concat(obj.media, " {");
  }
  var needLayer = typeof obj.layer !== "undefined";
  if (needLayer) {
    css += "@layer".concat(obj.layer.length > 0 ? " ".concat(obj.layer) : "", " {");
  }
  css += obj.css;
  if (needLayer) {
    css += "}";
  }
  if (obj.media) {
    css += "}";
  }
  if (obj.supports) {
    css += "}";
  }
  var sourceMap = obj.sourceMap;
  if (sourceMap && typeof btoa !== "undefined") {
    css += "\n/*# sourceMappingURL=data:application/json;base64,".concat(btoa(unescape(encodeURIComponent(JSON.stringify(sourceMap)))), " */");
  }

  // For old IE
  /* istanbul ignore if  */
  options.styleTagTransform(css, styleElement, options.options);
}
function removeStyleElement(styleElement) {
  // istanbul ignore if
  if (styleElement.parentNode === null) {
    return false;
  }
  styleElement.parentNode.removeChild(styleElement);
}

/* istanbul ignore next  */
function domAPI(options) {
  if (typeof document === "undefined") {
    return {
      update: function update() {},
      remove: function remove() {}
    };
  }
  var styleElement = options.insertStyleElement(options);
  return {
    update: function update(obj) {
      apply(styleElement, options, obj);
    },
    remove: function remove() {
      removeStyleElement(styleElement);
    }
  };
}
module.exports = domAPI;

/***/ }

/******/ 	});
/************************************************************************/
/******/ 	// The module cache
/******/ 	var __webpack_module_cache__ = {};
/******/ 	
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/ 		// Check if module is in cache
/******/ 		var cachedModule = __webpack_module_cache__[moduleId];
/******/ 		if (cachedModule !== undefined) {
/******/ 			return cachedModule.exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = __webpack_module_cache__[moduleId] = {
/******/ 			id: moduleId,
/******/ 			// no module.loaded needed
/******/ 			exports: {}
/******/ 		};
/******/ 	
/******/ 		// Execute the module function
/******/ 		__webpack_modules__[moduleId](module, module.exports, __webpack_require__);
/******/ 	
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/ 	
/************************************************************************/
/******/ 	/* webpack/runtime/compat get default export */
/******/ 	(() => {
/******/ 		// getDefaultExport function for compatibility with non-harmony modules
/******/ 		__webpack_require__.n = (module) => {
/******/ 			var getter = module && module.__esModule ?
/******/ 				() => (module['default']) :
/******/ 				() => (module);
/******/ 			__webpack_require__.d(getter, { a: getter });
/******/ 			return getter;
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/define property getters */
/******/ 	(() => {
/******/ 		// define getter functions for harmony exports
/******/ 		__webpack_require__.d = (exports, definition) => {
/******/ 			for(var key in definition) {
/******/ 				if(__webpack_require__.o(definition, key) && !__webpack_require__.o(exports, key)) {
/******/ 					Object.defineProperty(exports, key, { enumerable: true, get: definition[key] });
/******/ 				}
/******/ 			}
/******/ 		};
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/hasOwnProperty shorthand */
/******/ 	(() => {
/******/ 		__webpack_require__.o = (obj, prop) => (Object.prototype.hasOwnProperty.call(obj, prop))
/******/ 	})();
/******/ 	
/******/ 	/* webpack/runtime/nonce */
/******/ 	(() => {
/******/ 		__webpack_require__.nc = undefined;
/******/ 	})();
/******/ 	
/************************************************************************/
var __webpack_exports__ = {};

// EXPORTS
__webpack_require__.d(__webpack_exports__, {
  "default": () => (/* binding */ src)
});

// EXTERNAL MODULE: external "/res/sys/js/react.js"
var react_js_ = __webpack_require__(704);
var react_js_default = /*#__PURE__*/__webpack_require__.n(react_js_);
// EXTERNAL MODULE: external "/res/sys/js/react-dom.js"
var react_dom_js_ = __webpack_require__(739);
var react_dom_js_default = /*#__PURE__*/__webpack_require__.n(react_dom_js_);
;// ./src/api.ts
const BASE_URL = '/data/mqtt-sparkplug-publisher';
async function getBrokerConfig() {
    const response = await fetch(`${BASE_URL}/config/broker`);
    return response.json();
}
async function saveBrokerConfig(config) {
    const response = await fetch(`${BASE_URL}/config/broker`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    return response.json();
}
async function deleteBroker(id) {
    const response = await fetch(`${BASE_URL}/config/broker?id=${id}`, {
        method: 'DELETE'
    });
    return response.json();
}
async function getPublishConfig() {
    const response = await fetch(`${BASE_URL}/config/publish`);
    return response.json();
}
async function savePublishConfig(config) {
    const response = await fetch(`${BASE_URL}/config/publish`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
    });
    return response.json();
}

;// ./src/components/BrokerSettings.tsx


const BrokerSettings = () => {
    const [brokers, setBrokers] = (0,react_js_.useState)([]);
    const [selectedBrokerId, setSelectedBrokerId] = (0,react_js_.useState)(null);
    const [editingBroker, setEditingBroker] = (0,react_js_.useState)(null);
    const [loading, setLoading] = (0,react_js_.useState)(true);
    const [saving, setSaving] = (0,react_js_.useState)(false);
    const [message, setMessage] = (0,react_js_.useState)(null);
    (0,react_js_.useEffect)(() => {
        loadBrokers();
    }, []);
    const loadBrokers = async () => {
        setLoading(true);
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                if (response.data.length > 0 && !selectedBrokerId) {
                    selectBroker(response.data[0]);
                }
            }
        }
        catch (error) {
            setMessage({ type: 'error', text: 'Failed to load brokers' });
        }
        finally {
            setLoading(false);
        }
    };
    const selectBroker = (broker) => {
        setSelectedBrokerId(broker.id || null);
        setEditingBroker({ ...broker });
        setMessage(null);
    };
    const handleAddNew = () => {
        const newBroker = {
            name: 'New MQTT Broker',
            brokerUrl: 'tcp://localhost:1883',
            clientId: 'ignition-sparkplug-publisher',
            username: '',
            password: '',
            useTls: false,
            qos: 1,
            retained: false,
            cleanSession: true,
            connectionTimeout: 30,
            keepAliveInterval: 60,
            enabled: true
        };
        setEditingBroker(newBroker);
        setSelectedBrokerId(null);
        setMessage(null);
    };
    const handleChange = (e) => {
        if (!editingBroker)
            return;
        const { name, value, type } = e.target;
        if (type === 'checkbox') {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: e.target.checked
            } : null);
        }
        else if (type === 'number') {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: parseInt(value, 10)
            } : null);
        }
        else {
            setEditingBroker(prev => prev ? {
                ...prev,
                [name]: value
            } : null);
        }
    };
    const handleSave = async (e) => {
        e.preventDefault();
        if (!editingBroker)
            return;
        setSaving(true);
        setMessage(null);
        try {
            const response = await saveBrokerConfig(editingBroker);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Broker saved successfully' });
                await loadBrokers();
                selectBroker(response.data);
            }
            else {
                throw new Error(response.error || 'Failed to save broker');
            }
        }
        catch (error) {
            setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Failed to save broker' });
        }
        finally {
            setSaving(false);
        }
    };
    const handleDelete = async (brokerId) => {
        if (!confirm('Are you sure you want to delete this broker?')) {
            return;
        }
        const response = await deleteBroker(brokerId);
        if (response.success) {
            setMessage({ type: 'success', text: 'Broker deleted successfully' });
            await loadBrokers();
            setEditingBroker(null);
            setSelectedBrokerId(null);
        }
        else {
            setMessage({ type: 'error', text: response.error || 'Failed to delete broker' });
        }
    };
    if (loading) {
        return react_js_default().createElement("div", null, "Loading brokers...");
    }
    return (react_js_default().createElement("div", null,
        message && react_js_default().createElement("div", { className: `message ${message.type}` }, message.text),
        react_js_default().createElement("div", { className: "form-group" },
            react_js_default().createElement("label", null, "Brokers"),
            react_js_default().createElement("div", { className: "form-row" },
                react_js_default().createElement("select", { value: selectedBrokerId ?? '', onChange: (e) => {
                        const broker = brokers.find(b => b.id === Number(e.target.value));
                        if (broker) {
                            selectBroker(broker);
                        }
                    } },
                    react_js_default().createElement("option", { value: "" }, "Select a broker"),
                    brokers.map(broker => (react_js_default().createElement("option", { key: broker.id, value: broker.id }, broker.name)))),
                react_js_default().createElement("button", { type: "button", className: "btn-secondary", onClick: handleAddNew }, "New"))),
        editingBroker && (react_js_default().createElement("form", { onSubmit: handleSave },
            react_js_default().createElement("div", { className: "form-row" },
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Name"),
                    react_js_default().createElement("input", { name: "name", value: editingBroker.name, onChange: handleChange })),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Broker URL"),
                    react_js_default().createElement("input", { name: "brokerUrl", value: editingBroker.brokerUrl, onChange: handleChange })),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Client ID"),
                    react_js_default().createElement("input", { name: "clientId", value: editingBroker.clientId, onChange: handleChange }))),
            react_js_default().createElement("div", { className: "form-row" },
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Username"),
                    react_js_default().createElement("input", { name: "username", value: editingBroker.username || '', onChange: handleChange })),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Password"),
                    react_js_default().createElement("input", { name: "password", value: editingBroker.password || '', onChange: handleChange }))),
            react_js_default().createElement("div", { className: "form-row" },
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "QoS"),
                    react_js_default().createElement("input", { type: "number", name: "qos", value: editingBroker.qos, onChange: handleChange })),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Keep Alive (s)"),
                    react_js_default().createElement("input", { type: "number", name: "keepAliveInterval", value: editingBroker.keepAliveInterval, onChange: handleChange })),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", null, "Connection Timeout (s)"),
                    react_js_default().createElement("input", { type: "number", name: "connectionTimeout", value: editingBroker.connectionTimeout, onChange: handleChange }))),
            react_js_default().createElement("div", { className: "form-row" },
                react_js_default().createElement("label", null,
                    react_js_default().createElement("input", { type: "checkbox", name: "enabled", checked: editingBroker.enabled, onChange: handleChange }),
                    "Enabled"),
                react_js_default().createElement("label", null,
                    react_js_default().createElement("input", { type: "checkbox", name: "retained", checked: editingBroker.retained, onChange: handleChange }),
                    "Retained"),
                react_js_default().createElement("label", null,
                    react_js_default().createElement("input", { type: "checkbox", name: "cleanSession", checked: editingBroker.cleanSession, onChange: handleChange }),
                    "Clean Session")),
            react_js_default().createElement("div", { className: "mapping-actions" },
                react_js_default().createElement("button", { type: "submit", className: "btn-primary", disabled: saving }, saving ? 'Saving...' : 'Save Broker'),
                editingBroker.id && (react_js_default().createElement("button", { type: "button", className: "btn-danger", onClick: () => handleDelete(editingBroker.id) }, "Delete")))))));
};
/* harmony default export */ const components_BrokerSettings = (BrokerSettings);

;// ./src/components/SparkplugConfig.tsx


const SparkplugConfig = ({ config, onConfigSaved }) => {
    const [brokers, setBrokers] = (0,react_js_.useState)([]);
    const [formData, setFormData] = (0,react_js_.useState)(() => ({
        name: 'Default Sparkplug Publish',
        enabled: true,
        brokerId: 0,
        groupId: '',
        edgeNodeId: '',
        deviceMappings: []
    }));
    const [saving, setSaving] = (0,react_js_.useState)(false);
    const [message, setMessage] = (0,react_js_.useState)(null);
    const [validationErrors, setValidationErrors] = (0,react_js_.useState)([]);
    const [newMapping, setNewMapping] = (0,react_js_.useState)(() => ({
        sourcePattern: '',
        groupId: '',
        edgeNodeId: '',
        deviceId: '',
        enabled: true
    }));
    (0,react_js_.useEffect)(() => {
        loadBrokers();
    }, []);
    (0,react_js_.useEffect)(() => {
        if (config) {
            setFormData({
                ...config,
                deviceMappings: config.deviceMappings || []
            });
        }
    }, [config]);
    const loadBrokers = async () => {
        const response = await getBrokerConfig();
        if (response.success && response.data) {
            const brokerList = response.data;
            setBrokers(brokerList);
            if (!formData.brokerId && brokerList.length > 0) {
                setFormData(prev => ({ ...prev, brokerId: brokerList[0].id || 0 }));
            }
        }
    };
    const updateField = (key, value) => {
        setFormData(prev => ({
            ...prev,
            [key]: value
        }));
        if (validationErrors.length > 0) {
            setValidationErrors([]);
        }
    };
    const updateMapping = (index, key, value) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.map((mapping, idx) => (idx === index ? { ...mapping, [key]: value } : mapping))
        }));
        if (validationErrors.length > 0) {
            setValidationErrors([]);
        }
    };
    const addMapping = () => {
        const errors = [];
        const defaultGroupBlank = !formData.groupId || !formData.groupId.trim();
        const defaultEdgeBlank = !formData.edgeNodeId || !formData.edgeNodeId.trim();
        if (!newMapping.sourcePattern || !newMapping.sourcePattern.trim()) {
            errors.push('Tag folder is required for a new mapping.');
        }
        if (!newMapping.deviceId || !newMapping.deviceId.trim()) {
            errors.push('Device ID is required for a new mapping.');
        }
        if (defaultGroupBlank && (!newMapping.groupId || !newMapping.groupId.trim())) {
            errors.push('Group ID is required when defaults are blank.');
        }
        if (defaultEdgeBlank && (!newMapping.edgeNodeId || !newMapping.edgeNodeId.trim())) {
            errors.push('Edge Node ID is required when defaults are blank.');
        }
        if (errors.length > 0) {
            setValidationErrors(errors);
            setMessage({ type: 'error', text: 'Fix validation errors before adding a mapping.' });
            return;
        }
        const mappingToAdd = {
            sourcePattern: newMapping.sourcePattern.trim(),
            groupId: newMapping.groupId?.trim() || formData.groupId || '',
            edgeNodeId: newMapping.edgeNodeId?.trim() || formData.edgeNodeId || '',
            deviceId: newMapping.deviceId.trim(),
            enabled: newMapping.enabled
        };
        setFormData(prev => ({
            ...prev,
            deviceMappings: [...prev.deviceMappings, mappingToAdd]
        }));
        setNewMapping({
            sourcePattern: '',
            groupId: '',
            edgeNodeId: '',
            deviceId: '',
            enabled: true
        });
        setValidationErrors([]);
        setMessage(null);
    };
    const removeMapping = (index) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.filter((_, idx) => idx !== index)
        }));
    };
    const setAllMappingsEnabled = (enabled) => {
        setFormData(prev => ({
            ...prev,
            deviceMappings: prev.deviceMappings.map(mapping => ({
                ...mapping,
                enabled
            }))
        }));
    };
    const validateConfig = () => {
        const errors = [];
        if (!formData.name || !formData.name.trim()) {
            errors.push('Configuration name is required.');
        }
        if (!formData.brokerId) {
            errors.push('Select a broker for this configuration.');
        }
        const defaultGroup = formData.groupId?.trim() ?? '';
        const defaultEdge = formData.edgeNodeId?.trim() ?? '';
        const requiresMappingGroup = defaultGroup.length === 0;
        const requiresMappingEdge = defaultEdge.length === 0;
        formData.deviceMappings.forEach((mapping, index) => {
            const prefix = `Mapping ${index + 1}`;
            if (!mapping.sourcePattern || !mapping.sourcePattern.trim()) {
                errors.push(`${prefix}: Tag folder is required.`);
            }
            if (!mapping.deviceId || !mapping.deviceId.trim()) {
                errors.push(`${prefix}: Device ID is required.`);
            }
            if (requiresMappingGroup && (!mapping.groupId || !mapping.groupId.trim())) {
                errors.push(`${prefix}: Group ID is required when defaults are blank.`);
            }
            if (requiresMappingEdge && (!mapping.edgeNodeId || !mapping.edgeNodeId.trim())) {
                errors.push(`${prefix}: Edge Node ID is required when defaults are blank.`);
            }
        });
        return errors;
    };
    const handleSave = async (e) => {
        e.preventDefault();
        const errors = validateConfig();
        if (errors.length > 0) {
            setValidationErrors(errors);
            setMessage({ type: 'error', text: 'Fix validation errors before saving.' });
            return;
        }
        setValidationErrors([]);
        setSaving(true);
        setMessage(null);
        try {
            const response = await savePublishConfig(formData);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Sparkplug configuration saved.' });
                onConfigSaved(response.data);
            }
            else {
                throw new Error(response.error || 'Failed to save config');
            }
        }
        catch (error) {
            setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Failed to save config' });
        }
        finally {
            setSaving(false);
        }
    };
    const defaultGroupBlank = !formData.groupId || !formData.groupId.trim();
    const defaultEdgeBlank = !formData.edgeNodeId || !formData.edgeNodeId.trim();
    return (react_js_default().createElement("form", { onSubmit: handleSave },
        message && react_js_default().createElement("div", { className: `message ${message.type}` }, message.text),
        validationErrors.length > 0 && (react_js_default().createElement("div", { className: "validation-errors" },
            react_js_default().createElement("strong", null, "Missing required fields:"),
            react_js_default().createElement("ul", null, validationErrors.map((error, index) => (react_js_default().createElement("li", { key: `${error}-${index}` }, error)))))),
        react_js_default().createElement("div", { className: "form-row" },
            react_js_default().createElement("div", { className: "form-group" },
                react_js_default().createElement("label", null, "Name"),
                react_js_default().createElement("input", { value: formData.name, onChange: (e) => updateField('name', e.target.value) })),
            react_js_default().createElement("div", { className: "form-group" },
                react_js_default().createElement("label", null, "Broker"),
                react_js_default().createElement("select", { value: formData.brokerId, onChange: (e) => updateField('brokerId', Number(e.target.value)) }, brokers.map(broker => (react_js_default().createElement("option", { key: broker.id, value: broker.id }, broker.name))))),
            react_js_default().createElement("div", { className: "form-group" },
                react_js_default().createElement("label", null,
                    react_js_default().createElement("input", { type: "checkbox", checked: formData.enabled, onChange: (e) => updateField('enabled', e.target.checked) }),
                    "Enabled"))),
        react_js_default().createElement("div", { className: "form-row" },
            react_js_default().createElement("div", { className: "form-group" },
                react_js_default().createElement("label", null, "Default Group ID"),
                react_js_default().createElement("input", { value: formData.groupId || '', onChange: (e) => updateField('groupId', e.target.value) })),
            react_js_default().createElement("div", { className: "form-group" },
                react_js_default().createElement("label", null, "Default Edge Node ID"),
                react_js_default().createElement("input", { value: formData.edgeNodeId || '', onChange: (e) => updateField('edgeNodeId', e.target.value) }))),
        react_js_default().createElement("div", { className: "form-group" },
            react_js_default().createElement("label", null, "Device Mappings"),
            react_js_default().createElement("div", { className: "mapping-toolbar" },
                react_js_default().createElement("span", null,
                    formData.deviceMappings.length,
                    " mapping",
                    formData.deviceMappings.length === 1 ? '' : 's'),
                react_js_default().createElement("div", { className: "mapping-toolbar-actions" },
                    react_js_default().createElement("button", { type: "button", className: "btn-secondary", onClick: () => setAllMappingsEnabled(true) }, "Enable all"),
                    react_js_default().createElement("button", { type: "button", className: "btn-secondary", onClick: () => setAllMappingsEnabled(false) }, "Disable all"))),
            react_js_default().createElement("div", { className: "mapping-add" },
                react_js_default().createElement("div", { className: "mapping-add-row" },
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Tag Folder"),
                        react_js_default().createElement("input", { value: newMapping.sourcePattern, placeholder: "[default]Folder", onChange: (e) => setNewMapping(prev => ({ ...prev, sourcePattern: e.target.value })) })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Group ID"),
                        react_js_default().createElement("input", { value: newMapping.groupId, placeholder: formData.groupId || 'Default from config', onChange: (e) => setNewMapping(prev => ({ ...prev, groupId: e.target.value })) })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Edge Node ID"),
                        react_js_default().createElement("input", { value: newMapping.edgeNodeId, placeholder: formData.edgeNodeId || 'Default from config', onChange: (e) => setNewMapping(prev => ({ ...prev, edgeNodeId: e.target.value })) })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Device ID"),
                        react_js_default().createElement("input", { value: newMapping.deviceId, placeholder: "Device", onChange: (e) => setNewMapping(prev => ({ ...prev, deviceId: e.target.value })) })),
                    react_js_default().createElement("div", { className: "form-group mapping-add-actions" },
                        react_js_default().createElement("label", null,
                            react_js_default().createElement("input", { type: "checkbox", checked: newMapping.enabled, onChange: (e) => setNewMapping(prev => ({ ...prev, enabled: e.target.checked })) }),
                            "Enabled"),
                        react_js_default().createElement("button", { type: "button", className: "btn-secondary", onClick: addMapping }, "+ Add Mapping")))),
            react_js_default().createElement("div", { className: "mapping-list" }, formData.deviceMappings.map((mapping, index) => (react_js_default().createElement("div", { key: index, className: `mapping-item ${mapping.enabled ? '' : 'disabled'}` },
                react_js_default().createElement("div", { className: "mapping-summary" },
                    react_js_default().createElement("div", { className: "mapping-summary-text" },
                        react_js_default().createElement("span", { className: "mapping-summary-label" }, mapping.sourcePattern || 'New mapping'),
                        react_js_default().createElement("span", { className: "mapping-summary-arrow" }, "\u2192"),
                        react_js_default().createElement("span", { className: "mapping-summary-label" }, mapping.deviceId || 'Device')),
                    react_js_default().createElement("div", { className: "mapping-summary-actions" },
                        react_js_default().createElement("label", { className: "mapping-toggle" },
                            react_js_default().createElement("input", { type: "checkbox", checked: mapping.enabled, onChange: (e) => updateMapping(index, 'enabled', e.target.checked) })),
                        react_js_default().createElement("button", { type: "button", className: "btn-danger", onClick: () => removeMapping(index) }, "Delete"))),
                react_js_default().createElement("div", { className: "form-row" },
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Tag Folder"),
                        react_js_default().createElement("input", { value: mapping.sourcePattern, onChange: (e) => updateMapping(index, 'sourcePattern', e.target.value) })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Group ID"),
                        react_js_default().createElement("input", { value: mapping.groupId || '', onChange: (e) => updateMapping(index, 'groupId', e.target.value) }),
                        defaultGroupBlank && (!mapping.groupId || !mapping.groupId.trim()) && (react_js_default().createElement("small", { className: "field-warning" }, "Required when defaults are blank"))),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Edge Node ID"),
                        react_js_default().createElement("input", { value: mapping.edgeNodeId || '', onChange: (e) => updateMapping(index, 'edgeNodeId', e.target.value) }),
                        defaultEdgeBlank && (!mapping.edgeNodeId || !mapping.edgeNodeId.trim()) && (react_js_default().createElement("small", { className: "field-warning" }, "Required when defaults are blank"))),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", null, "Device ID"),
                        react_js_default().createElement("input", { value: mapping.deviceId, onChange: (e) => updateMapping(index, 'deviceId', e.target.value) }))))))),
            react_js_default().createElement("button", { type: "button", className: "btn-secondary", onClick: addMapping }, "+ Add Mapping")),
        react_js_default().createElement("button", { type: "submit", className: "btn-primary", disabled: saving }, saving ? 'Saving...' : 'Save Sparkplug Config')));
};
/* harmony default export */ const components_SparkplugConfig = (SparkplugConfig);

// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/injectStylesIntoStyleTag.js
var injectStylesIntoStyleTag = __webpack_require__(72);
var injectStylesIntoStyleTag_default = /*#__PURE__*/__webpack_require__.n(injectStylesIntoStyleTag);
// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/styleDomAPI.js
var styleDomAPI = __webpack_require__(825);
var styleDomAPI_default = /*#__PURE__*/__webpack_require__.n(styleDomAPI);
// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/insertBySelector.js
var insertBySelector = __webpack_require__(659);
var insertBySelector_default = /*#__PURE__*/__webpack_require__.n(insertBySelector);
// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/setAttributesWithoutAttributes.js
var setAttributesWithoutAttributes = __webpack_require__(56);
var setAttributesWithoutAttributes_default = /*#__PURE__*/__webpack_require__.n(setAttributesWithoutAttributes);
// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/insertStyleElement.js
var insertStyleElement = __webpack_require__(540);
var insertStyleElement_default = /*#__PURE__*/__webpack_require__.n(insertStyleElement);
// EXTERNAL MODULE: ./node_modules/style-loader/dist/runtime/styleTagTransform.js
var styleTagTransform = __webpack_require__(113);
var styleTagTransform_default = /*#__PURE__*/__webpack_require__.n(styleTagTransform);
// EXTERNAL MODULE: ./node_modules/css-loader/dist/cjs.js!./src/styles.css
var styles = __webpack_require__(365);
;// ./src/styles.css

      
      
      
      
      
      
      
      
      

var options = {};

options.styleTagTransform = (styleTagTransform_default());
options.setAttributes = (setAttributesWithoutAttributes_default());

      options.insert = insertBySelector_default().bind(null, "head");
    
options.domAPI = (styleDomAPI_default());
options.insertStyleElement = (insertStyleElement_default());

var update = injectStylesIntoStyleTag_default()(styles/* default */.A, options);




       /* harmony default export */ const src_styles = (styles/* default */.A && styles/* default */.A.locals ? styles/* default */.A.locals : undefined);

;// ./src/components/Configuration.tsx





const Configuration = () => {
    const [activeTab, setActiveTab] = (0,react_js_.useState)('broker');
    const [publishConfig, setPublishConfig] = (0,react_js_.useState)(null);
    const [loading, setLoading] = (0,react_js_.useState)(true);
    const [error, setError] = (0,react_js_.useState)(null);
    (0,react_js_.useEffect)(() => {
        loadConfiguration();
    }, []);
    const loadConfiguration = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await getPublishConfig();
            if (response.success && response.data) {
                const config = response.data[0] || null;
                setPublishConfig(config);
            }
            else if (!response.success) {
                throw new Error(response.error || 'Failed to load Sparkplug configuration');
            }
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load configuration');
        }
        finally {
            setLoading(false);
        }
    };
    const handlePublishConfigSaved = (newConfig) => {
        setPublishConfig(newConfig);
    };
    if (loading) {
        return (react_js_default().createElement("div", { className: "mqtt-config-page" },
            react_js_default().createElement("div", null, "Loading configuration...")));
    }
    if (error) {
        return (react_js_default().createElement("div", { className: "mqtt-config-page" },
            react_js_default().createElement("div", { className: "message error" },
                react_js_default().createElement("strong", null, "Error:"),
                " ",
                error,
                react_js_default().createElement("button", { onClick: loadConfiguration, className: "btn-secondary" }, "Retry"))));
    }
    return (react_js_default().createElement("div", { className: "mqtt-config-page" },
        react_js_default().createElement("header", { className: "page-header" },
            react_js_default().createElement("h1", null, "MQTT SparkplugB Publisher"),
            react_js_default().createElement("p", { className: "page-description" }, "Configure brokers and SparkplugB tag mappings.")),
        react_js_default().createElement("div", { className: "tabs" },
            react_js_default().createElement("button", { className: `tab ${activeTab === 'broker' ? 'active' : ''}`, onClick: () => setActiveTab('broker') }, "Broker Configuration"),
            react_js_default().createElement("button", { className: `tab ${activeTab === 'sparkplug' ? 'active' : ''}`, onClick: () => setActiveTab('sparkplug') }, "Sparkplug Mapping")),
        react_js_default().createElement("div", { className: "tab-content" },
            activeTab === 'broker' && (react_js_default().createElement(components_BrokerSettings, null)),
            activeTab === 'sparkplug' && (react_js_default().createElement(components_SparkplugConfig, { config: publishConfig, onConfigSaved: handlePublishConfigSaved })))));
};
/* harmony default export */ const components_Configuration = (Configuration);

;// ./src/components/ErrorBoundary.tsx

class ErrorBoundary extends react_js_.Component {
    constructor(props) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null
        };
    }
    static getDerivedStateFromError(error) {
        return {
            hasError: true,
            error,
            errorInfo: null
        };
    }
    componentDidCatch(error, errorInfo) {
        this.setState({
            error,
            errorInfo
        });
    }
    render() {
        if (this.state.hasError) {
            return (react_js_default().createElement("div", { style: { padding: '20px', backgroundColor: '#fee', border: '2px solid #c00', borderRadius: '4px' } },
                react_js_default().createElement("h2", { style: { color: '#c00', margin: '0 0 10px 0' } }, "Component Error"),
                react_js_default().createElement("p", null,
                    react_js_default().createElement("strong", null, "Error:"),
                    " ",
                    this.state.error?.message || 'Unknown error'),
                react_js_default().createElement("details", { style: { marginTop: '10px' } },
                    react_js_default().createElement("summary", { style: { cursor: 'pointer', fontWeight: 'bold' } }, "Stack Trace"),
                    react_js_default().createElement("pre", { style: {
                            marginTop: '10px',
                            padding: '10px',
                            backgroundColor: '#f5f5f5',
                            overflow: 'auto',
                            fontSize: '12px'
                        } }, this.state.error?.stack)),
                this.state.errorInfo && (react_js_default().createElement("details", { style: { marginTop: '10px' } },
                    react_js_default().createElement("summary", { style: { cursor: 'pointer', fontWeight: 'bold' } }, "Component Stack"),
                    react_js_default().createElement("pre", { style: {
                            marginTop: '10px',
                            padding: '10px',
                            backgroundColor: '#f5f5f5',
                            overflow: 'auto',
                            fontSize: '12px'
                        } }, this.state.errorInfo.componentStack)))));
        }
        return this.props.children;
    }
}
/* harmony default export */ const components_ErrorBoundary = (ErrorBoundary);

;// ./src/index.tsx




console.log('=== SPARKPLUG MODULE LOADING ===');
console.log('[Sparkplug Module] index.tsx loaded');
console.log('[Sparkplug Module] React available:', typeof (react_js_default()) !== 'undefined');
console.log('[Sparkplug Module] ReactDOM available:', typeof (react_dom_js_default()) !== 'undefined');
console.log('[Sparkplug Module] ConfigurationComponent type:', typeof components_Configuration);
const ConfigurationWrapper = function (...args) {
    console.log('[Sparkplug Module] Configuration wrapper called');
    console.log('[Sparkplug Module] this:', this);
    console.log('[Sparkplug Module] args:', args);
    console.log('[Sparkplug Module] args.length:', args.length);
    if (new.target) {
        console.log('[Sparkplug Module] Called with new keyword (constructor)');
        return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) });
    }
    if (args.length === 0) {
        console.log('[Sparkplug Module] Called with no args - returning React element');
        return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) });
    }
    if (args.length >= 1) {
        console.log('[Sparkplug Module] Called with args - creating React element with props');
        const props = args[0] && typeof args[0] === 'object' ? args[0] : {};
        return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration, props) });
    }
    if (args[0] && args[0].nodeType) {
        console.log('[Sparkplug Module] Called with DOM element - mounting component');
        react_dom_js_default().render(react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) }), args[0]);
        return;
    }
    console.log('[Sparkplug Module] Fallback - returning React element');
    return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) });
};
ConfigurationWrapper.displayName = 'Configuration';
const moduleExports = {
    Configuration: ConfigurationWrapper
};
console.log('[Sparkplug Module] Module exports:', moduleExports);
console.log('=== SPARKPLUG MODULE EXPORT ===');
if (typeof window !== 'undefined') {
    setTimeout(() => {
        const System = window.System;
        if (System) {
            const registered = System.get('com.inductiveautomation.mqtt.sparkplugb.gateway');
            console.log('[Sparkplug Module] SystemJS registration check:', registered);
        }
    }, 500);
}
/* harmony default export */ const src = (moduleExports);

__webpack_exports__ = __webpack_exports__["default"];
/******/ 	return __webpack_exports__;
/******/ })()
;
});
//# sourceMappingURL=sparkplug-config.js.map