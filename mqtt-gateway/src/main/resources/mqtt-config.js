define("mqtt-config", ["React"], (__WEBPACK_EXTERNAL_MODULE__359__) => { return /******/ (() => { // webpackBootstrap
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

/***/ 359
(module) {

module.exports = __WEBPACK_EXTERNAL_MODULE__359__;

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
___CSS_LOADER_EXPORT___.push([module.id, `/* MQTT UNS Publisher Gateway Configuration UI Styles */

.mqtt-config-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page-header {
    margin-bottom: 30px;
}

.page-header h1 {
    margin: 0 0 10px 0;
    font-size: 28px;
    color: #333;
}

.page-description {
    margin: 0;
    color: #666;
    font-size: 14px;
}

/* Loading and Error States */
.loading {
    text-align: center;
    padding: 40px;
    color: #666;
}

.error-banner {
    background: #fee;
    border: 1px solid #fcc;
    border-radius: 4px;
    padding: 15px;
    margin-bottom: 20px;
    color: #c00;
}

.btn-retry {
    margin-left: 10px;
    padding: 4px 12px;
    background: white;
    border: 1px solid #c00;
    border-radius: 3px;
    color: #c00;
    cursor: pointer;
}

.btn-retry:hover {
    background: #c00;
    color: white;
}

/* Tabs */
.tabs {
    display: flex;
    gap: 5px;
    margin-bottom: 20px;
    border-bottom: 2px solid #ddd;
}

.tab {
    padding: 12px 24px;
    background: #f5f5f5;
    border: 1px solid #ddd;
    border-bottom: none;
    border-radius: 4px 4px 0 0;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.tab:hover {
    background: #e8e8e8;
}

.tab.active {
    background: white;
    border-bottom: 2px solid white;
    margin-bottom: -2px;
    font-weight: 600;
}

.tab-content {
    background: white;
    padding: 30px;
    border: 1px solid #ddd;
    border-radius: 0 4px 4px 4px;
}

/* Forms */
.form-section {
    margin-bottom: 30px;
}

.form-section h2 {
    margin: 0 0 20px 0;
    font-size: 20px;
    color: #333;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
}

.form-group {
    margin-bottom: 20px;
}

.form-group label {
    display: block;
    margin-bottom: 6px;
    font-weight: 500;
    font-size: 14px;
    color: #333;
}

.form-group input[type="text"],
.form-group input[type="password"],
.form-group input[type="number"],
.form-group select,
.form-group textarea {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    box-sizing: border-box;
}

.form-group small {
    display: block;
    margin-top: 4px;
    color: #666;
    font-size: 12px;
}

.form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.form-group.checkbox label {
    display: flex;
    align-items: center;
    font-weight: normal;
}

.form-group.checkbox input[type="checkbox"] {
    margin-right: 8px;
    width: auto;
}

/* List Input */
.list-input {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
}

.list-input input {
    flex: 1;
}

.btn-add {
    padding: 8px 16px;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    white-space: nowrap;
}

.btn-add:hover {
    background: #0056b3;
}

.item-list {
    list-style: none;
    padding: 0;
    margin: 10px 0;
    border: 1px solid #ddd;
    border-radius: 4px;
    max-height: 200px;
    overflow-y: auto;
}

.item-list li {
    padding: 10px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.item-list li:last-child {
    border-bottom: none;
}

.btn-remove {
    padding: 4px 12px;
    background: #dc3545;
    color: white;
    border: none;
    border-radius: 3px;
    font-size: 12px;
    cursor: pointer;
}

.btn-remove:hover {
    background: #c82333;
}

/* Messages */
.message {
    padding: 12px;
    border-radius: 4px;
    margin: 20px 0;
}

.message.success {
    background: #d4edda;
    border: 1px solid #c3e6cb;
    color: #155724;
}

.message.error {
    background: #f8d7da;
    border: 1px solid #f5c6cb;
    color: #721c24;
}

.message.info {
    background: #d1ecf1;
    border: 1px solid #bee5eb;
    color: #0c5460;
}

/* Form Actions */
.form-actions {
    display: flex;
    gap: 10px;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #eee;
}

.btn-primary,
.btn-secondary {
    padding: 10px 24px;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
}

.btn-primary {
    background: #28a745;
    color: white;
}

.btn-primary:hover:not(:disabled) {
    background: #218838;
}

.btn-secondary {
    background: #6c757d;
    color: white;
}

.btn-secondary:hover:not(:disabled) {
    background: #5a6268;
}

.btn-primary:disabled,
.btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* Status Dashboard */
.status-dashboard {
    min-height: 400px;
}

.status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.auto-refresh {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #666;
}

.auto-refresh input[type="checkbox"] {
    margin: 0;
}

.status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.status-card {
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    padding: 20px;
}

.status-card h3 {
    margin: 0 0 15px 0;
    font-size: 16px;
    color: #495057;
}

.status-content {
    text-align: center;
}

.badge {
    display: inline-block;
    padding: 6px 12px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 600;
    text-transform: uppercase;
}

.badge-success {
    background: #d4edda;
    color: #155724;
}

.badge-warning {
    background: #fff3cd;
    color: #856404;
}

.badge-error {
    background: #f8d7da;
    color: #721c24;
}

.badge-unknown {
    background: #e2e3e5;
    color: #383d41;
}

.status-message {
    margin: 10px 0 0 0;
    font-size: 14px;
    color: #6c757d;
}

.broker-url {
    margin: 10px 0 0 0;
    font-size: 13px;
    font-family: monospace;
    color: #495057;
}

.reconnect-info {
    margin: 8px 0 0 0;
    font-size: 12px;
    color: #856404;
}

.stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #212529;
    margin-bottom: 5px;
}

.stat-label {
    margin: 0;
    font-size: 13px;
    color: #6c757d;
}

/* Statistics Section */
.statistics-section {
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    padding: 20px;
}

.statistics-section h3 {
    margin: 0 0 20px 0;
    font-size: 18px;
    color: #333;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.stat-item {
    display: flex;
    flex-direction: column;
}

.stat-item label {
    font-size: 13px;
    color: #6c757d;
    margin-bottom: 5px;
}

.stat-item .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #212529;
}

.stat-item .stat-value.error {
    color: #dc3545;
}
`, "",{"version":3,"sources":["webpack://./src/styles.css"],"names":[],"mappings":"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","sourcesContent":["/* MQTT UNS Publisher Gateway Configuration UI Styles */\n\n.mqtt-config-page {\n    max-width: 1200px;\n    margin: 0 auto;\n    padding: 20px;\n    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n}\n\n.page-header {\n    margin-bottom: 30px;\n}\n\n.page-header h1 {\n    margin: 0 0 10px 0;\n    font-size: 28px;\n    color: #333;\n}\n\n.page-description {\n    margin: 0;\n    color: #666;\n    font-size: 14px;\n}\n\n/* Loading and Error States */\n.loading {\n    text-align: center;\n    padding: 40px;\n    color: #666;\n}\n\n.error-banner {\n    background: #fee;\n    border: 1px solid #fcc;\n    border-radius: 4px;\n    padding: 15px;\n    margin-bottom: 20px;\n    color: #c00;\n}\n\n.btn-retry {\n    margin-left: 10px;\n    padding: 4px 12px;\n    background: white;\n    border: 1px solid #c00;\n    border-radius: 3px;\n    color: #c00;\n    cursor: pointer;\n}\n\n.btn-retry:hover {\n    background: #c00;\n    color: white;\n}\n\n/* Tabs */\n.tabs {\n    display: flex;\n    gap: 5px;\n    margin-bottom: 20px;\n    border-bottom: 2px solid #ddd;\n}\n\n.tab {\n    padding: 12px 24px;\n    background: #f5f5f5;\n    border: 1px solid #ddd;\n    border-bottom: none;\n    border-radius: 4px 4px 0 0;\n    cursor: pointer;\n    font-size: 14px;\n    transition: all 0.2s;\n}\n\n.tab:hover {\n    background: #e8e8e8;\n}\n\n.tab.active {\n    background: white;\n    border-bottom: 2px solid white;\n    margin-bottom: -2px;\n    font-weight: 600;\n}\n\n.tab-content {\n    background: white;\n    padding: 30px;\n    border: 1px solid #ddd;\n    border-radius: 0 4px 4px 4px;\n}\n\n/* Forms */\n.form-section {\n    margin-bottom: 30px;\n}\n\n.form-section h2 {\n    margin: 0 0 20px 0;\n    font-size: 20px;\n    color: #333;\n    border-bottom: 1px solid #eee;\n    padding-bottom: 10px;\n}\n\n.form-group {\n    margin-bottom: 20px;\n}\n\n.form-group label {\n    display: block;\n    margin-bottom: 6px;\n    font-weight: 500;\n    font-size: 14px;\n    color: #333;\n}\n\n.form-group input[type=\"text\"],\n.form-group input[type=\"password\"],\n.form-group input[type=\"number\"],\n.form-group select,\n.form-group textarea {\n    width: 100%;\n    padding: 8px 12px;\n    border: 1px solid #ddd;\n    border-radius: 4px;\n    font-size: 14px;\n    box-sizing: border-box;\n}\n\n.form-group small {\n    display: block;\n    margin-top: 4px;\n    color: #666;\n    font-size: 12px;\n}\n\n.form-row {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n    gap: 20px;\n}\n\n.form-group.checkbox label {\n    display: flex;\n    align-items: center;\n    font-weight: normal;\n}\n\n.form-group.checkbox input[type=\"checkbox\"] {\n    margin-right: 8px;\n    width: auto;\n}\n\n/* List Input */\n.list-input {\n    display: flex;\n    gap: 10px;\n    margin-bottom: 10px;\n}\n\n.list-input input {\n    flex: 1;\n}\n\n.btn-add {\n    padding: 8px 16px;\n    background: #007bff;\n    color: white;\n    border: none;\n    border-radius: 4px;\n    cursor: pointer;\n    white-space: nowrap;\n}\n\n.btn-add:hover {\n    background: #0056b3;\n}\n\n.item-list {\n    list-style: none;\n    padding: 0;\n    margin: 10px 0;\n    border: 1px solid #ddd;\n    border-radius: 4px;\n    max-height: 200px;\n    overflow-y: auto;\n}\n\n.item-list li {\n    padding: 10px;\n    border-bottom: 1px solid #eee;\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n}\n\n.item-list li:last-child {\n    border-bottom: none;\n}\n\n.btn-remove {\n    padding: 4px 12px;\n    background: #dc3545;\n    color: white;\n    border: none;\n    border-radius: 3px;\n    font-size: 12px;\n    cursor: pointer;\n}\n\n.btn-remove:hover {\n    background: #c82333;\n}\n\n/* Messages */\n.message {\n    padding: 12px;\n    border-radius: 4px;\n    margin: 20px 0;\n}\n\n.message.success {\n    background: #d4edda;\n    border: 1px solid #c3e6cb;\n    color: #155724;\n}\n\n.message.error {\n    background: #f8d7da;\n    border: 1px solid #f5c6cb;\n    color: #721c24;\n}\n\n.message.info {\n    background: #d1ecf1;\n    border: 1px solid #bee5eb;\n    color: #0c5460;\n}\n\n/* Form Actions */\n.form-actions {\n    display: flex;\n    gap: 10px;\n    margin-top: 30px;\n    padding-top: 20px;\n    border-top: 1px solid #eee;\n}\n\n.btn-primary,\n.btn-secondary {\n    padding: 10px 24px;\n    border: none;\n    border-radius: 4px;\n    font-size: 14px;\n    font-weight: 500;\n    cursor: pointer;\n    transition: all 0.2s;\n}\n\n.btn-primary {\n    background: #28a745;\n    color: white;\n}\n\n.btn-primary:hover:not(:disabled) {\n    background: #218838;\n}\n\n.btn-secondary {\n    background: #6c757d;\n    color: white;\n}\n\n.btn-secondary:hover:not(:disabled) {\n    background: #5a6268;\n}\n\n.btn-primary:disabled,\n.btn-secondary:disabled {\n    opacity: 0.5;\n    cursor: not-allowed;\n}\n\n/* Status Dashboard */\n.status-dashboard {\n    min-height: 400px;\n}\n\n.status-header {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    margin-bottom: 20px;\n}\n\n.auto-refresh {\n    display: flex;\n    align-items: center;\n    gap: 8px;\n    font-size: 14px;\n    color: #666;\n}\n\n.auto-refresh input[type=\"checkbox\"] {\n    margin: 0;\n}\n\n.status-grid {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\n    gap: 20px;\n    margin-bottom: 30px;\n}\n\n.status-card {\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 8px;\n    padding: 20px;\n}\n\n.status-card h3 {\n    margin: 0 0 15px 0;\n    font-size: 16px;\n    color: #495057;\n}\n\n.status-content {\n    text-align: center;\n}\n\n.badge {\n    display: inline-block;\n    padding: 6px 12px;\n    border-radius: 4px;\n    font-size: 14px;\n    font-weight: 600;\n    text-transform: uppercase;\n}\n\n.badge-success {\n    background: #d4edda;\n    color: #155724;\n}\n\n.badge-warning {\n    background: #fff3cd;\n    color: #856404;\n}\n\n.badge-error {\n    background: #f8d7da;\n    color: #721c24;\n}\n\n.badge-unknown {\n    background: #e2e3e5;\n    color: #383d41;\n}\n\n.status-message {\n    margin: 10px 0 0 0;\n    font-size: 14px;\n    color: #6c757d;\n}\n\n.broker-url {\n    margin: 10px 0 0 0;\n    font-size: 13px;\n    font-family: monospace;\n    color: #495057;\n}\n\n.reconnect-info {\n    margin: 8px 0 0 0;\n    font-size: 12px;\n    color: #856404;\n}\n\n.stat-value {\n    font-size: 32px;\n    font-weight: 700;\n    color: #212529;\n    margin-bottom: 5px;\n}\n\n.stat-label {\n    margin: 0;\n    font-size: 13px;\n    color: #6c757d;\n}\n\n/* Statistics Section */\n.statistics-section {\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 8px;\n    padding: 20px;\n}\n\n.statistics-section h3 {\n    margin: 0 0 20px 0;\n    font-size: 18px;\n    color: #333;\n}\n\n.stats-grid {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n    gap: 20px;\n}\n\n.stat-item {\n    display: flex;\n    flex-direction: column;\n}\n\n.stat-item label {\n    font-size: 13px;\n    color: #6c757d;\n    margin-bottom: 5px;\n}\n\n.stat-item .stat-value {\n    font-size: 24px;\n    font-weight: 600;\n    color: #212529;\n}\n\n.stat-item .stat-value.error {\n    color: #dc3545;\n}\n"],"sourceRoot":""}]);
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

// EXTERNAL MODULE: external "React"
var external_React_ = __webpack_require__(359);
var external_React_default = /*#__PURE__*/__webpack_require__.n(external_React_);
;// ./src/api.ts
/**
 * API client for MQTT UNS Publisher configuration and status endpoints
 */
const API_BASE = '/data/mqtt-uns-publisher';
/**
 * Generic fetch wrapper with error handling
 */
async function apiFetch(url, options) {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers
            }
        });
        const data = await response.json();
        if (!response.ok) {
            return {
                success: false,
                error: data.error || `HTTP ${response.status}: ${response.statusText}`
            };
        }
        return data;
    }
    catch (error) {
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Network error'
        };
    }
}
/**
 * Get broker configuration
 */
async function getBrokerConfig() {
    return apiFetch(`${API_BASE}/config/broker`);
}
/**
 * Save broker configuration
 */
async function saveBrokerConfig(config) {
    return apiFetch(`${API_BASE}/config/broker`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}
/**
 * Get tag configuration
 */
async function getTagConfig() {
    return apiFetch(`${API_BASE}/config/tags`);
}
/**
 * Save tag configuration
 */
async function saveTagConfig(config) {
    return apiFetch(`${API_BASE}/config/tags`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}
/**
 * Get module status and statistics
 */
async function getModuleStatus() {
    return apiFetch(`${API_BASE}/status`);
}
/**
 * Test MQTT broker connection
 */
async function testConnection(config) {
    return apiFetch(`${API_BASE}/test-connection`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}
/**
 * Get all configuration (broker + tags)
 */
async function getAllConfig() {
    return apiFetch(`${API_BASE}/config`);
}

;// ./src/components/BrokerSettings.tsx


const BrokerSettings = ({ config, onConfigSaved }) => {
    const [formData, setFormData] = (0,external_React_.useState)({
        brokerUrl: 'tcp://localhost:1883',
        clientId: 'ignition-mqtt-publisher',
        username: '',
        password: '',
        useTls: false,
        qos: 1,
        retained: false,
        cleanSession: true,
        connectionTimeout: 30,
        keepAliveInterval: 60,
        enabled: false
    });
    const [saving, setSaving] = (0,external_React_.useState)(false);
    const [testing, setTesting] = (0,external_React_.useState)(false);
    const [message, setMessage] = (0,external_React_.useState)(null);
    (0,external_React_.useEffect)(() => {
        if (config) {
            setFormData(config);
        }
    }, [config]);
    const handleChange = (e) => {
        const { name, value, type } = e.target;
        if (type === 'checkbox') {
            setFormData(prev => ({
                ...prev,
                [name]: e.target.checked
            }));
        }
        else if (type === 'number') {
            setFormData(prev => ({
                ...prev,
                [name]: parseInt(value, 10)
            }));
        }
        else {
            setFormData(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };
    const handleTestConnection = async () => {
        setTesting(true);
        setMessage(null);
        const testRequest = {
            brokerUrl: formData.brokerUrl,
            clientId: formData.clientId,
            username: formData.username,
            password: formData.password,
            useTls: formData.useTls,
            connectionTimeout: formData.connectionTimeout,
            keepAliveInterval: formData.keepAliveInterval,
            cleanSession: formData.cleanSession
        };
        try {
            const response = await testConnection(testRequest);
            if (response.success && response.data?.connected) {
                setMessage({
                    type: 'success',
                    text: `Connection successful! (${response.data.connectionTimeMs}ms)`
                });
            }
            else {
                setMessage({
                    type: 'error',
                    text: response.data?.message || response.error || 'Connection test failed'
                });
            }
        }
        catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Connection test failed'
            });
        }
        finally {
            setTesting(false);
        }
    };
    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);
        try {
            const response = await saveBrokerConfig(formData);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Configuration saved successfully' });
                onConfigSaved(response.data);
            }
            else {
                throw new Error(response.error || 'Failed to save configuration');
            }
        }
        catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to save configuration'
            });
        }
        finally {
            setSaving(false);
        }
    };
    return (external_React_default().createElement("div", { className: "broker-settings" },
        external_React_default().createElement("form", { onSubmit: handleSave },
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "Connection Settings"),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", { htmlFor: "brokerUrl" }, "Broker URL"),
                    external_React_default().createElement("input", { type: "text", id: "brokerUrl", name: "brokerUrl", value: formData.brokerUrl, onChange: handleChange, placeholder: "tcp://localhost:1883", required: true }),
                    external_React_default().createElement("small", null, "Format: tcp://hostname:port or ssl://hostname:port")),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", { htmlFor: "clientId" }, "Client ID"),
                    external_React_default().createElement("input", { type: "text", id: "clientId", name: "clientId", value: formData.clientId, onChange: handleChange, placeholder: "ignition-mqtt-publisher", required: true })),
                external_React_default().createElement("div", { className: "form-row" },
                    external_React_default().createElement("div", { className: "form-group" },
                        external_React_default().createElement("label", { htmlFor: "username" }, "Username (optional)"),
                        external_React_default().createElement("input", { type: "text", id: "username", name: "username", value: formData.username || '', onChange: handleChange, placeholder: "Leave empty for anonymous" })),
                    external_React_default().createElement("div", { className: "form-group" },
                        external_React_default().createElement("label", { htmlFor: "password" }, "Password (optional)"),
                        external_React_default().createElement("input", { type: "password", id: "password", name: "password", value: formData.password || '', onChange: handleChange, placeholder: "Leave empty for no password" })))),
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "MQTT Settings"),
                external_React_default().createElement("div", { className: "form-row" },
                    external_React_default().createElement("div", { className: "form-group" },
                        external_React_default().createElement("label", { htmlFor: "qos" }, "Quality of Service (QoS)"),
                        external_React_default().createElement("select", { id: "qos", name: "qos", value: formData.qos, onChange: handleChange },
                            external_React_default().createElement("option", { value: 0 }, "0 - At most once"),
                            external_React_default().createElement("option", { value: 1 }, "1 - At least once"),
                            external_React_default().createElement("option", { value: 2 }, "2 - Exactly once"))),
                    external_React_default().createElement("div", { className: "form-group" },
                        external_React_default().createElement("label", { htmlFor: "connectionTimeout" }, "Connection Timeout (seconds)"),
                        external_React_default().createElement("input", { type: "number", id: "connectionTimeout", name: "connectionTimeout", value: formData.connectionTimeout, onChange: handleChange, min: 5, max: 300 })),
                    external_React_default().createElement("div", { className: "form-group" },
                        external_React_default().createElement("label", { htmlFor: "keepAliveInterval" }, "Keep Alive Interval (seconds)"),
                        external_React_default().createElement("input", { type: "number", id: "keepAliveInterval", name: "keepAliveInterval", value: formData.keepAliveInterval, onChange: handleChange, min: 10, max: 3600 }))),
                external_React_default().createElement("div", { className: "form-row" },
                    external_React_default().createElement("div", { className: "form-group checkbox" },
                        external_React_default().createElement("label", null,
                            external_React_default().createElement("input", { type: "checkbox", name: "useTls", checked: formData.useTls, onChange: handleChange }),
                            "Use TLS/SSL encryption")),
                    external_React_default().createElement("div", { className: "form-group checkbox" },
                        external_React_default().createElement("label", null,
                            external_React_default().createElement("input", { type: "checkbox", name: "retained", checked: formData.retained, onChange: handleChange }),
                            "Retain messages on broker")),
                    external_React_default().createElement("div", { className: "form-group checkbox" },
                        external_React_default().createElement("label", null,
                            external_React_default().createElement("input", { type: "checkbox", name: "cleanSession", checked: formData.cleanSession, onChange: handleChange }),
                            "Clean session on connect")))),
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "Module Settings"),
                external_React_default().createElement("div", { className: "form-group checkbox" },
                    external_React_default().createElement("label", null,
                        external_React_default().createElement("input", { type: "checkbox", name: "enabled", checked: formData.enabled, onChange: handleChange }),
                        external_React_default().createElement("strong", null, "Enable MQTT publishing")),
                    external_React_default().createElement("small", null, "Module must be enabled to publish messages to MQTT broker"))),
            message && (external_React_default().createElement("div", { className: `message ${message.type}` }, message.text)),
            external_React_default().createElement("div", { className: "form-actions" },
                external_React_default().createElement("button", { type: "button", onClick: handleTestConnection, disabled: testing || saving, className: "btn-secondary" }, testing ? 'Testing...' : 'Test Connection'),
                external_React_default().createElement("button", { type: "submit", disabled: saving || testing, className: "btn-primary" }, saving ? 'Saving...' : 'Save Configuration')))));
};
/* harmony default export */ const components_BrokerSettings = (BrokerSettings);

;// ./src/components/TagSelection.tsx


const TagSelection = ({ config, onConfigSaved }) => {
    const [formData, setFormData] = (0,external_React_.useState)({
        name: 'Default Tag Publishing',
        enabled: false,
        tagProviders: ['default'],
        tagFolders: [],
        topicOverrides: {},
        payloadTemplate: '',
        includeMetadata: true,
        valueDeadband: 0.1,
        publishOnQualityChange: true
    });
    const [saving, setSaving] = (0,external_React_.useState)(false);
    const [message, setMessage] = (0,external_React_.useState)(null);
    const [newProvider, setNewProvider] = (0,external_React_.useState)('');
    const [newFolder, setNewFolder] = (0,external_React_.useState)('');
    (0,external_React_.useEffect)(() => {
        if (config) {
            setFormData(config);
        }
    }, [config]);
    const handleChange = (e) => {
        const { name, value, type } = e.target;
        if (type === 'checkbox') {
            setFormData(prev => ({
                ...prev,
                [name]: e.target.checked
            }));
        }
        else if (type === 'number') {
            setFormData(prev => ({
                ...prev,
                [name]: parseFloat(value)
            }));
        }
        else {
            setFormData(prev => ({
                ...prev,
                [name]: value
            }));
        }
    };
    const addProvider = () => {
        if (newProvider && !formData.tagProviders.includes(newProvider)) {
            setFormData(prev => ({
                ...prev,
                tagProviders: [...prev.tagProviders, newProvider]
            }));
            setNewProvider('');
        }
    };
    const removeProvider = (provider) => {
        setFormData(prev => ({
            ...prev,
            tagProviders: prev.tagProviders.filter(p => p !== provider)
        }));
    };
    const addFolder = () => {
        if (newFolder && !formData.tagFolders.includes(newFolder)) {
            setFormData(prev => ({
                ...prev,
                tagFolders: [...prev.tagFolders, newFolder]
            }));
            setNewFolder('');
        }
    };
    const removeFolder = (folder) => {
        setFormData(prev => ({
            ...prev,
            tagFolders: prev.tagFolders.filter(f => f !== folder)
        }));
    };
    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);
        try {
            const response = await saveTagConfig(formData);
            if (response.success && response.data) {
                setMessage({ type: 'success', text: 'Tag configuration saved successfully' });
                onConfigSaved(response.data);
            }
            else {
                throw new Error(response.error || 'Failed to save configuration');
            }
        }
        catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to save configuration'
            });
        }
        finally {
            setSaving(false);
        }
    };
    return (external_React_default().createElement("div", { className: "tag-selection" },
        external_React_default().createElement("form", { onSubmit: handleSave },
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "Tag Selection"),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", null, "Tag Providers"),
                    external_React_default().createElement("div", { className: "list-input" },
                        external_React_default().createElement("input", { type: "text", value: newProvider, onChange: (e) => setNewProvider(e.target.value), placeholder: "Enter provider name (e.g., default)", onKeyPress: (e) => e.key === 'Enter' && (e.preventDefault(), addProvider()) }),
                        external_React_default().createElement("button", { type: "button", onClick: addProvider, className: "btn-add" }, "Add Provider")),
                    external_React_default().createElement("ul", { className: "item-list" }, formData.tagProviders.map(provider => (external_React_default().createElement("li", { key: provider },
                        provider,
                        external_React_default().createElement("button", { type: "button", onClick: () => removeProvider(provider), className: "btn-remove" }, "Remove"))))),
                    external_React_default().createElement("small", null, "Specify which tag providers to monitor for changes")),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", null, "Tag Folders"),
                    external_React_default().createElement("div", { className: "list-input" },
                        external_React_default().createElement("input", { type: "text", value: newFolder, onChange: (e) => setNewFolder(e.target.value), placeholder: "Enter folder path (e.g., [default]Folder/Subfolder)", onKeyPress: (e) => e.key === 'Enter' && (e.preventDefault(), addFolder()) }),
                        external_React_default().createElement("button", { type: "button", onClick: addFolder, className: "btn-add" }, "Add Folder")),
                    external_React_default().createElement("ul", { className: "item-list" }, formData.tagFolders.map(folder => (external_React_default().createElement("li", { key: folder },
                        folder,
                        external_React_default().createElement("button", { type: "button", onClick: () => removeFolder(folder), className: "btn-remove" }, "Remove"))))),
                    external_React_default().createElement("small", null, "Specify folders to monitor. Leave empty to monitor all tags in selected providers."))),
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "Publishing Settings"),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", { htmlFor: "valueDeadband" }, "Value Deadband"),
                    external_React_default().createElement("input", { type: "number", id: "valueDeadband", name: "valueDeadband", value: formData.valueDeadband, onChange: handleChange, min: 0, step: 0.01 }),
                    external_React_default().createElement("small", null, "Minimum change required to publish (prevents noise from small fluctuations)")),
                external_React_default().createElement("div", { className: "form-group" },
                    external_React_default().createElement("label", { htmlFor: "payloadTemplate" }, "Payload Template (optional)"),
                    external_React_default().createElement("textarea", { id: "payloadTemplate", name: "payloadTemplate", value: formData.payloadTemplate || '', onChange: handleChange, placeholder: "Leave empty for default JSON format", rows: 4 }),
                    external_React_default().createElement("small", null, "Custom JSON template for message payload (advanced users only)")),
                external_React_default().createElement("div", { className: "form-group checkbox" },
                    external_React_default().createElement("label", null,
                        external_React_default().createElement("input", { type: "checkbox", name: "includeMetadata", checked: formData.includeMetadata, onChange: handleChange }),
                        "Include metadata (timestamp, quality, datatype)")),
                external_React_default().createElement("div", { className: "form-group checkbox" },
                    external_React_default().createElement("label", null,
                        external_React_default().createElement("input", { type: "checkbox", name: "publishOnQualityChange", checked: formData.publishOnQualityChange, onChange: handleChange }),
                        "Publish when tag quality changes"))),
            external_React_default().createElement("div", { className: "form-section" },
                external_React_default().createElement("h2", null, "Module Settings"),
                external_React_default().createElement("div", { className: "form-group checkbox" },
                    external_React_default().createElement("label", null,
                        external_React_default().createElement("input", { type: "checkbox", name: "enabled", checked: formData.enabled, onChange: handleChange }),
                        external_React_default().createElement("strong", null, "Enable tag publishing")),
                    external_React_default().createElement("small", null, "Tag publishing must be enabled to monitor and publish tag changes"))),
            message && (external_React_default().createElement("div", { className: `message ${message.type}` }, message.text)),
            external_React_default().createElement("div", { className: "form-actions" },
                external_React_default().createElement("button", { type: "submit", disabled: saving, className: "btn-primary" }, saving ? 'Saving...' : 'Save Configuration')))));
};
/* harmony default export */ const components_TagSelection = (TagSelection);

;// ./src/components/StatusDashboard.tsx


const StatusDashboard = () => {
    const [status, setStatus] = (0,external_React_.useState)(null);
    const [loading, setLoading] = (0,external_React_.useState)(true);
    const [error, setError] = (0,external_React_.useState)(null);
    const [autoRefresh, setAutoRefresh] = (0,external_React_.useState)(true);
    (0,external_React_.useEffect)(() => {
        loadStatus();
        if (autoRefresh) {
            const interval = setInterval(loadStatus, 5000); // Refresh every 5 seconds
            return () => clearInterval(interval);
        }
    }, [autoRefresh]);
    const loadStatus = async () => {
        try {
            const response = await getModuleStatus();
            if (response.success && response.data) {
                setStatus(response.data);
                setError(null);
            }
            else {
                throw new Error(response.error || 'Failed to load status');
            }
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load status');
        }
        finally {
            setLoading(false);
        }
    };
    const getHealthBadgeClass = (healthLevel) => {
        switch (healthLevel) {
            case 'HEALTHY':
                return 'badge-success';
            case 'DEGRADED':
                return 'badge-warning';
            case 'UNHEALTHY':
                return 'badge-error';
            default:
                return 'badge-unknown';
        }
    };
    const getConnectionBadgeClass = (connectionState) => {
        switch (connectionState) {
            case 'CONNECTED':
                return 'badge-success';
            case 'CONNECTING':
            case 'RECONNECTING':
                return 'badge-warning';
            case 'DISCONNECTED':
            case 'ERROR':
                return 'badge-error';
            default:
                return 'badge-unknown';
        }
    };
    if (loading) {
        return (external_React_default().createElement("div", { className: "status-dashboard" },
            external_React_default().createElement("div", { className: "loading" }, "Loading status...")));
    }
    if (error) {
        return (external_React_default().createElement("div", { className: "status-dashboard" },
            external_React_default().createElement("div", { className: "error-banner" },
                external_React_default().createElement("strong", null, "Error:"),
                " ",
                error,
                external_React_default().createElement("button", { onClick: loadStatus, className: "btn-retry" }, "Retry"))));
    }
    if (!status) {
        return null;
    }
    return (external_React_default().createElement("div", { className: "status-dashboard" },
        external_React_default().createElement("div", { className: "status-header" },
            external_React_default().createElement("h2", null, "Module Status"),
            external_React_default().createElement("label", { className: "auto-refresh" },
                external_React_default().createElement("input", { type: "checkbox", checked: autoRefresh, onChange: (e) => setAutoRefresh(e.target.checked) }),
                "Auto-refresh (5s)")),
        external_React_default().createElement("div", { className: "status-grid" },
            external_React_default().createElement("div", { className: "status-card" },
                external_React_default().createElement("h3", null, "Health Status"),
                external_React_default().createElement("div", { className: "status-content" },
                    external_React_default().createElement("div", { className: `badge ${getHealthBadgeClass(status.healthLevel)}` }, status.healthLevel || 'UNKNOWN'),
                    external_React_default().createElement("p", { className: "status-message" }, status.statusMessage))),
            external_React_default().createElement("div", { className: "status-card" },
                external_React_default().createElement("h3", null, "MQTT Connection"),
                external_React_default().createElement("div", { className: "status-content" },
                    external_React_default().createElement("div", { className: `badge ${getConnectionBadgeClass(status.connectionState)}` }, status.connectionStateDisplay || status.connectionState || 'UNKNOWN'),
                    status.brokerUrl && external_React_default().createElement("p", { className: "broker-url" }, status.brokerUrl),
                    status.reconnectAttempts !== undefined && status.reconnectAttempts > 0 && (external_React_default().createElement("p", { className: "reconnect-info" },
                        "Reconnect attempts: ",
                        status.reconnectAttempts)))),
            external_React_default().createElement("div", { className: "status-card" },
                external_React_default().createElement("h3", null, "Tag Monitoring"),
                external_React_default().createElement("div", { className: "status-content" },
                    external_React_default().createElement("div", { className: "stat-value" }, status.monitoredTagCount),
                    external_React_default().createElement("p", { className: "stat-label" }, "Monitored Tags"))),
            external_React_default().createElement("div", { className: "status-card" },
                external_React_default().createElement("h3", null, "Uptime"),
                external_React_default().createElement("div", { className: "status-content" },
                    external_React_default().createElement("div", { className: "stat-value" }, status.statistics.uptimeDisplay),
                    external_React_default().createElement("p", { className: "stat-label" },
                        status.statistics.uptimeMs.toLocaleString(),
                        " ms")))),
        external_React_default().createElement("div", { className: "statistics-section" },
            external_React_default().createElement("h3", null, "Publishing Statistics"),
            external_React_default().createElement("div", { className: "stats-grid" },
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Messages Published"),
                    external_React_default().createElement("span", { className: "stat-value" }, status.statistics.messagesPublished.toLocaleString())),
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Messages Failed"),
                    external_React_default().createElement("span", { className: `stat-value ${status.statistics.messagesFailed > 0 ? 'error' : ''}` }, status.statistics.messagesFailed.toLocaleString())),
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Publish Success Rate"),
                    external_React_default().createElement("span", { className: "stat-value" },
                        status.statistics.publishSuccessRate.toFixed(1),
                        "%")),
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Tag Reads Successful"),
                    external_React_default().createElement("span", { className: "stat-value" }, status.statistics.tagReadsSuccessful.toLocaleString())),
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Tag Reads Failed"),
                    external_React_default().createElement("span", { className: `stat-value ${status.statistics.tagReadsFailed > 0 ? 'error' : ''}` }, status.statistics.tagReadsFailed.toLocaleString())),
                external_React_default().createElement("div", { className: "stat-item" },
                    external_React_default().createElement("label", null, "Tag Read Success Rate"),
                    external_React_default().createElement("span", { className: "stat-value" },
                        status.statistics.tagReadSuccessRate.toFixed(1),
                        "%"))))));
};
/* harmony default export */ const components_StatusDashboard = (StatusDashboard);

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
    const [activeTab, setActiveTab] = (0,external_React_.useState)('broker');
    const [brokerConfig, setBrokerConfig] = (0,external_React_.useState)(null);
    const [tagConfig, setTagConfig] = (0,external_React_.useState)(null);
    const [loading, setLoading] = (0,external_React_.useState)(true);
    const [error, setError] = (0,external_React_.useState)(null);
    (0,external_React_.useEffect)(() => {
        loadConfiguration();
    }, []);
    const loadConfiguration = async () => {
        setLoading(true);
        setError(null);
        try {
            // Load broker config
            const brokerResponse = await getBrokerConfig();
            if (brokerResponse.success && brokerResponse.data) {
                setBrokerConfig(brokerResponse.data);
            }
            else if (!brokerResponse.success) {
                throw new Error(brokerResponse.error || 'Failed to load broker configuration');
            }
            // Load tag config
            const tagResponse = await getTagConfig();
            if (tagResponse.success && tagResponse.data) {
                setTagConfig(tagResponse.data);
            }
            else if (!tagResponse.success) {
                throw new Error(tagResponse.error || 'Failed to load tag configuration');
            }
        }
        catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load configuration');
        }
        finally {
            setLoading(false);
        }
    };
    const handleBrokerConfigSaved = (newConfig) => {
        setBrokerConfig(newConfig);
    };
    const handleTagConfigSaved = (newConfig) => {
        setTagConfig(newConfig);
    };
    if (loading) {
        return (external_React_default().createElement("div", { className: "mqtt-config-page" },
            external_React_default().createElement("div", { className: "loading" }, "Loading configuration...")));
    }
    if (error) {
        return (external_React_default().createElement("div", { className: "mqtt-config-page" },
            external_React_default().createElement("div", { className: "error-banner" },
                external_React_default().createElement("strong", null, "Error:"),
                " ",
                error,
                external_React_default().createElement("button", { onClick: loadConfiguration, className: "btn-retry" }, "Retry"))));
    }
    return (external_React_default().createElement("div", { className: "mqtt-config-page" },
        external_React_default().createElement("header", { className: "page-header" },
            external_React_default().createElement("h1", null, "MQTT UNS Publisher Configuration"),
            external_React_default().createElement("p", { className: "page-description" }, "Configure MQTT broker connection and tag publishing settings")),
        external_React_default().createElement("div", { className: "tabs" },
            external_React_default().createElement("button", { className: `tab ${activeTab === 'broker' ? 'active' : ''}`, onClick: () => setActiveTab('broker') }, "Broker Settings"),
            external_React_default().createElement("button", { className: `tab ${activeTab === 'tags' ? 'active' : ''}`, onClick: () => setActiveTab('tags') }, "Tag Publishing"),
            external_React_default().createElement("button", { className: `tab ${activeTab === 'status' ? 'active' : ''}`, onClick: () => setActiveTab('status') }, "Status & Statistics")),
        external_React_default().createElement("div", { className: "tab-content" },
            activeTab === 'broker' && (external_React_default().createElement(components_BrokerSettings, { config: brokerConfig, onConfigSaved: handleBrokerConfigSaved })),
            activeTab === 'tags' && (external_React_default().createElement(components_TagSelection, { config: tagConfig, onConfigSaved: handleTagConfigSaved })),
            activeTab === 'status' && (external_React_default().createElement(components_StatusDashboard, null)))));
};
/* harmony default export */ const components_Configuration = (Configuration);

;// ./src/index.tsx


// Debug logging to help diagnose the issue
console.log('[MQTT Module] index.tsx loaded');
console.log('[MQTT Module] React available:', typeof (external_React_default()) !== 'undefined');
console.log('[MQTT Module] ConfigurationComponent type:', typeof components_Configuration);
// For Ignition Gateway SystemJS loading
// The .mount() method expects module.Configuration
// Export as default with Configuration property
const moduleExports = {
    Configuration: components_Configuration
};
// Log after export
console.log('[MQTT Module] Module exports:', moduleExports);
console.log('[MQTT Module] Configuration type:', typeof moduleExports.Configuration);
/* harmony default export */ const src = (moduleExports);

__webpack_exports__ = __webpack_exports__["default"];
/******/ 	return __webpack_exports__;
/******/ })()
;
});;
//# sourceMappingURL=mqtt-config.js.map