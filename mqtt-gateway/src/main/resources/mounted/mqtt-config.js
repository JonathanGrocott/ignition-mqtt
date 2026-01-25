(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("http://localhost:8088/res/sys/js/react.js"), require("http://localhost:8088/res/sys/js/react-dom.js"));
	else if(typeof define === 'function' && define.amd)
		define("com.inductiveautomation.mqtt.uns.gateway", ["http://localhost:8088/res/sys/js/react.js", "http://localhost:8088/res/sys/js/react-dom.js"], factory);
	else if(typeof exports === 'object')
		exports["com.inductiveautomation.mqtt.uns.gateway"] = factory(require("http://localhost:8088/res/sys/js/react.js"), require("http://localhost:8088/res/sys/js/react-dom.js"));
	else
		root["com.inductiveautomation.mqtt.uns.gateway"] = factory(root["http://localhost:8088/res/sys/js/react.js"], root["http://localhost:8088/res/sys/js/react-dom.js"]);
})(this, (__WEBPACK_EXTERNAL_MODULE__341__, __WEBPACK_EXTERNAL_MODULE__554__) => {
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

/***/ 341
(module) {

module.exports = __WEBPACK_EXTERNAL_MODULE__341__;

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

.broker-counts {
    margin: 10px 0 0 0;
    font-size: 14px;
    font-weight: 500;
    color: #495057;
}

.hint-text {
    margin: 8px 0 0 0;
    font-size: 12px;
    color: #6c757d;
    font-style: italic;
}

.hint-text strong {
    font-weight: 600;
    font-style: normal;
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

/* Topic Mappings */
.section-description {
    font-size: 14px;
    color: #666;
    margin: 0 0 15px 0;
    line-height: 1.5;
}

.section-description code {
    background: #f5f5f5;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 13px;
}

.mapping-input {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 15px;
}

.mapping-input .mapping-source {
    flex: 1;
    min-width: 0;
}

.mapping-input .mapping-topic {
    flex: 1.5;
    min-width: 0;
}

.mapping-input .mapping-arrow {
    color: #666;
    font-size: 18px;
    font-weight: bold;
    flex-shrink: 0;
}

.mappings-list {
    border: 1px solid #dee2e6;
    border-radius: 4px;
    margin: 15px 0;
    background: #f8f9fa;
}

.mapping-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 15px;
    border-bottom: 1px solid #dee2e6;
    background: white;
    transition: background 0.2s;
}

.mapping-item:last-child {
    border-bottom: none;
}

.mapping-item:hover {
    background: #f8f9fa;
}

.mapping-item.disabled {
    opacity: 0.5;
    background: #f5f5f5;
}

.mapping-details {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 13px;
}

.mapping-source-display {
    color: #0066cc;
    font-weight: 500;
}

.mapping-topic-display {
    color: #28a745;
    font-weight: 500;
}

.mapping-actions {
    display: flex;
    align-items: center;
    gap: 10px;
}

.toggle-switch {
    position: relative;
    display: inline-block;
    width: 44px;
    height: 24px;
    margin: 0;
}

.toggle-switch input {
    opacity: 0;
    width: 0;
    height: 0;
}

.toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: 0.3s;
    border-radius: 24px;
}

.toggle-slider:before {
    position: absolute;
    content: "";
    height: 18px;
    width: 18px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
}

.toggle-switch input:checked + .toggle-slider {
    background-color: #28a745;
}

.toggle-switch input:checked + .toggle-slider:before {
    transform: translateX(20px);
}

.btn-remove-small {
    background: none;
    border: none;
    color: #dc3545;
    font-size: 20px;
    line-height: 1;
    cursor: pointer;
    padding: 0 5px;
    transition: color 0.2s;
}

.btn-remove-small:hover {
    color: #c82333;
}

.no-mappings {
    padding: 20px;
    text-align: center;
    color: #666;
    background: #f8f9fa;
    border: 1px dashed #dee2e6;
    border-radius: 4px;
    margin: 10px 0;
}

.no-mappings code {
    background: white;
    padding: 3px 8px;
    border-radius: 3px;
    font-family: 'Monaco', 'Courier New', monospace;
    color: #0066cc;
}

/* Active Subscriptions View */
.subscriptions-view {
    margin-top: 30px;
}

.subscriptions-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
}

.subscriptions-count {
    color: #666;
    font-size: 14px;
}

.btn-refresh {
    padding: 6px 12px;
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
}

.btn-refresh:hover {
    background: #e9ecef;
    border-color: #adb5bd;
}

.subscriptions-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
    border: 1px solid #dee2e6;
    border-radius: 4px;
    overflow: hidden;
}

.subscriptions-table th {
    background: #f8f9fa;
    padding: 12px;
    text-align: left;
    font-weight: 600;
    color: #495057;
    border-bottom: 2px solid #dee2e6;
    font-size: 13px;
}

.subscriptions-table td {
    padding: 10px 12px;
    border-bottom: 1px solid #dee2e6;
    font-size: 13px;
}

.subscriptions-table tr:last-child td {
    border-bottom: none;
}

.subscriptions-table tr:hover {
    background: #f8f9fa;
}

.tag-path {
    font-family: 'Monaco', 'Courier New', monospace;
    color: #0066cc;
}

.mqtt-topic {
    font-family: 'Monaco', 'Courier New', monospace;
    color: #28a745;
    font-size: 12px;
}

.publish-count {
    text-align: right;
    color: #666;
}

.last-published {
    color: #999;
    font-size: 12px;
}

.quality-good {
    color: #28a745;
    font-weight: 500;
}

.quality-bad {
    color: #dc3545;
    font-weight: 500;
}

.quality-uncertain {
    color: #ffc107;
    font-weight: 500;
}

/* Multi-Broker Settings Layout */
.broker-settings.multi-broker {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 20px;
    height: calc(100vh - 250px);
    min-height: 600px;
}

.broker-list-panel {
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.broker-list-panel .panel-header {
    padding: 15px;
    border-bottom: 1px solid #dee2e6;
    background: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.broker-list-panel .panel-header h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
}

.broker-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
}

.broker-item {
    background: white;
    border: 2px solid #dee2e6;
    border-radius: 6px;
    padding: 12px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.2s;
}

.broker-item:hover {
    border-color: #007bff;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.broker-item.selected {
    border-color: #007bff;
    background: #e7f3ff;
}

.broker-item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
}

.broker-item-header strong {
    font-size: 14px;
    color: #333;
}

.broker-item-url {
    font-size: 12px;
    color: #666;
    font-family: 'Monaco', 'Courier New', monospace;
}

.badge {
    font-size: 10px;
    padding: 2px 8px;
    border-radius: 10px;
    font-weight: 600;
    text-transform: uppercase;
}

.badge-success {
    background: #d4edda;
    color: #155724;
}

.broker-edit-panel {
    background: white;
    border: 1px solid #dee2e6;
    border-radius: 6px;
    padding: 20px;
    overflow-y: auto;
}

.broker-edit-panel .panel-header {
    margin: -20px -20px 20px -20px;
    padding: 15px 20px;
    border-bottom: 1px solid #dee2e6;
    background: #f8f9fa;
}

.broker-edit-panel .panel-header h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.broker-edit-panel .empty-state {
    text-align: center;
    color: #999;
    padding: 60px 20px;
}

.btn-small {
    font-size: 12px;
    padding: 6px 12px;
}

.form-actions .left-actions {
    flex: 1;
}

.form-actions .right-actions {
    display: flex;
    gap: 10px;
}

.btn-danger {
    background: #dc3545;
    color: white;
    border: 1px solid #dc3545;
}

.btn-danger:hover {
    background: #c82333;
    border-color: #bd2130;
}

/* Topic Mappings by Broker */
.mapping-input-container {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.mapping-input-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.broker-select {
    flex: 1;
    padding: 8px;
    border: 1px solid #ced4da;
    border-radius: 4px;
    font-size: 14px;
}

.mappings-by-broker {
    margin-top: 20px;
}

.broker-mappings-group {
    margin-bottom: 30px;
}

.broker-mappings-group.unassigned-group {
    background: #fff3cd;
    border: 2px solid #ffc107;
    border-radius: 8px;
    padding: 15px;
}

.broker-mappings-group.unassigned-group .broker-group-header {
    border-bottom-color: #ffc107;
    color: #856404;
}

.warning-text {
    font-size: 13px;
    color: #856404;
    margin: 0 0 12px 0;
    font-style: italic;
}

.broker-group-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 2px solid #007bff;
    font-size: 16px;
    color: #333;
}

.broker-group-header .broker-url {
    font-size: 13px;
    color: #666;
    font-family: 'Monaco', 'Courier New', monospace;
    font-weight: normal;
}

.broker-group-header .mapping-count {
    font-size: 12px;
    color: #999;
    font-weight: normal;
    margin-left: auto;
}

.warning-message {
    background: #fff3cd;
    border: 1px solid #ffc107;
    border-radius: 4px;
    padding: 12px;
    color: #856404;
    margin-bottom: 15px;
}

.warning-message strong {
    display: block;
    margin-bottom: 4px;
}
`, "",{"version":3,"sources":["webpack://./src/styles.css"],"names":[],"mappings":"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","sourcesContent":["/* MQTT UNS Publisher Gateway Configuration UI Styles */\n\n.mqtt-config-page {\n    max-width: 1200px;\n    margin: 0 auto;\n    padding: 20px;\n    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n}\n\n.page-header {\n    margin-bottom: 30px;\n}\n\n.page-header h1 {\n    margin: 0 0 10px 0;\n    font-size: 28px;\n    color: #333;\n}\n\n.page-description {\n    margin: 0;\n    color: #666;\n    font-size: 14px;\n}\n\n/* Loading and Error States */\n.loading {\n    text-align: center;\n    padding: 40px;\n    color: #666;\n}\n\n.error-banner {\n    background: #fee;\n    border: 1px solid #fcc;\n    border-radius: 4px;\n    padding: 15px;\n    margin-bottom: 20px;\n    color: #c00;\n}\n\n.btn-retry {\n    margin-left: 10px;\n    padding: 4px 12px;\n    background: white;\n    border: 1px solid #c00;\n    border-radius: 3px;\n    color: #c00;\n    cursor: pointer;\n}\n\n.btn-retry:hover {\n    background: #c00;\n    color: white;\n}\n\n/* Tabs */\n.tabs {\n    display: flex;\n    gap: 5px;\n    margin-bottom: 20px;\n    border-bottom: 2px solid #ddd;\n}\n\n.tab {\n    padding: 12px 24px;\n    background: #f5f5f5;\n    border: 1px solid #ddd;\n    border-bottom: none;\n    border-radius: 4px 4px 0 0;\n    cursor: pointer;\n    font-size: 14px;\n    transition: all 0.2s;\n}\n\n.tab:hover {\n    background: #e8e8e8;\n}\n\n.tab.active {\n    background: white;\n    border-bottom: 2px solid white;\n    margin-bottom: -2px;\n    font-weight: 600;\n}\n\n.tab-content {\n    background: white;\n    padding: 30px;\n    border: 1px solid #ddd;\n    border-radius: 0 4px 4px 4px;\n}\n\n/* Forms */\n.form-section {\n    margin-bottom: 30px;\n}\n\n.form-section h2 {\n    margin: 0 0 20px 0;\n    font-size: 20px;\n    color: #333;\n    border-bottom: 1px solid #eee;\n    padding-bottom: 10px;\n}\n\n.form-group {\n    margin-bottom: 20px;\n}\n\n.form-group label {\n    display: block;\n    margin-bottom: 6px;\n    font-weight: 500;\n    font-size: 14px;\n    color: #333;\n}\n\n.form-group input[type=\"text\"],\n.form-group input[type=\"password\"],\n.form-group input[type=\"number\"],\n.form-group select,\n.form-group textarea {\n    width: 100%;\n    padding: 8px 12px;\n    border: 1px solid #ddd;\n    border-radius: 4px;\n    font-size: 14px;\n    box-sizing: border-box;\n}\n\n.form-group small {\n    display: block;\n    margin-top: 4px;\n    color: #666;\n    font-size: 12px;\n}\n\n.form-row {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n    gap: 20px;\n}\n\n.form-group.checkbox label {\n    display: flex;\n    align-items: center;\n    font-weight: normal;\n}\n\n.form-group.checkbox input[type=\"checkbox\"] {\n    margin-right: 8px;\n    width: auto;\n}\n\n/* List Input */\n.list-input {\n    display: flex;\n    gap: 10px;\n    margin-bottom: 10px;\n}\n\n.list-input input {\n    flex: 1;\n}\n\n.btn-add {\n    padding: 8px 16px;\n    background: #007bff;\n    color: white;\n    border: none;\n    border-radius: 4px;\n    cursor: pointer;\n    white-space: nowrap;\n}\n\n.btn-add:hover {\n    background: #0056b3;\n}\n\n.item-list {\n    list-style: none;\n    padding: 0;\n    margin: 10px 0;\n    border: 1px solid #ddd;\n    border-radius: 4px;\n    max-height: 200px;\n    overflow-y: auto;\n}\n\n.item-list li {\n    padding: 10px;\n    border-bottom: 1px solid #eee;\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n}\n\n.item-list li:last-child {\n    border-bottom: none;\n}\n\n.btn-remove {\n    padding: 4px 12px;\n    background: #dc3545;\n    color: white;\n    border: none;\n    border-radius: 3px;\n    font-size: 12px;\n    cursor: pointer;\n}\n\n.btn-remove:hover {\n    background: #c82333;\n}\n\n/* Messages */\n.message {\n    padding: 12px;\n    border-radius: 4px;\n    margin: 20px 0;\n}\n\n.message.success {\n    background: #d4edda;\n    border: 1px solid #c3e6cb;\n    color: #155724;\n}\n\n.message.error {\n    background: #f8d7da;\n    border: 1px solid #f5c6cb;\n    color: #721c24;\n}\n\n.message.info {\n    background: #d1ecf1;\n    border: 1px solid #bee5eb;\n    color: #0c5460;\n}\n\n/* Form Actions */\n.form-actions {\n    display: flex;\n    gap: 10px;\n    margin-top: 30px;\n    padding-top: 20px;\n    border-top: 1px solid #eee;\n}\n\n.btn-primary,\n.btn-secondary {\n    padding: 10px 24px;\n    border: none;\n    border-radius: 4px;\n    font-size: 14px;\n    font-weight: 500;\n    cursor: pointer;\n    transition: all 0.2s;\n}\n\n.btn-primary {\n    background: #28a745;\n    color: white;\n}\n\n.btn-primary:hover:not(:disabled) {\n    background: #218838;\n}\n\n.btn-secondary {\n    background: #6c757d;\n    color: white;\n}\n\n.btn-secondary:hover:not(:disabled) {\n    background: #5a6268;\n}\n\n.btn-primary:disabled,\n.btn-secondary:disabled {\n    opacity: 0.5;\n    cursor: not-allowed;\n}\n\n/* Status Dashboard */\n.status-dashboard {\n    min-height: 400px;\n}\n\n.status-header {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    margin-bottom: 20px;\n}\n\n.auto-refresh {\n    display: flex;\n    align-items: center;\n    gap: 8px;\n    font-size: 14px;\n    color: #666;\n}\n\n.auto-refresh input[type=\"checkbox\"] {\n    margin: 0;\n}\n\n.status-grid {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\n    gap: 20px;\n    margin-bottom: 30px;\n}\n\n.status-card {\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 8px;\n    padding: 20px;\n}\n\n.status-card h3 {\n    margin: 0 0 15px 0;\n    font-size: 16px;\n    color: #495057;\n}\n\n.status-content {\n    text-align: center;\n}\n\n.badge {\n    display: inline-block;\n    padding: 6px 12px;\n    border-radius: 4px;\n    font-size: 14px;\n    font-weight: 600;\n    text-transform: uppercase;\n}\n\n.badge-success {\n    background: #d4edda;\n    color: #155724;\n}\n\n.badge-warning {\n    background: #fff3cd;\n    color: #856404;\n}\n\n.badge-error {\n    background: #f8d7da;\n    color: #721c24;\n}\n\n.badge-unknown {\n    background: #e2e3e5;\n    color: #383d41;\n}\n\n.status-message {\n    margin: 10px 0 0 0;\n    font-size: 14px;\n    color: #6c757d;\n}\n\n.broker-url {\n    margin: 10px 0 0 0;\n    font-size: 13px;\n    font-family: monospace;\n    color: #495057;\n}\n\n.broker-counts {\n    margin: 10px 0 0 0;\n    font-size: 14px;\n    font-weight: 500;\n    color: #495057;\n}\n\n.hint-text {\n    margin: 8px 0 0 0;\n    font-size: 12px;\n    color: #6c757d;\n    font-style: italic;\n}\n\n.hint-text strong {\n    font-weight: 600;\n    font-style: normal;\n    color: #495057;\n}\n\n.reconnect-info {\n    margin: 8px 0 0 0;\n    font-size: 12px;\n    color: #856404;\n}\n\n.stat-value {\n    font-size: 32px;\n    font-weight: 700;\n    color: #212529;\n    margin-bottom: 5px;\n}\n\n.stat-label {\n    margin: 0;\n    font-size: 13px;\n    color: #6c757d;\n}\n\n/* Statistics Section */\n.statistics-section {\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 8px;\n    padding: 20px;\n}\n\n.statistics-section h3 {\n    margin: 0 0 20px 0;\n    font-size: 18px;\n    color: #333;\n}\n\n.stats-grid {\n    display: grid;\n    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n    gap: 20px;\n}\n\n.stat-item {\n    display: flex;\n    flex-direction: column;\n}\n\n.stat-item label {\n    font-size: 13px;\n    color: #6c757d;\n    margin-bottom: 5px;\n}\n\n.stat-item .stat-value {\n    font-size: 24px;\n    font-weight: 600;\n    color: #212529;\n}\n\n.stat-item .stat-value.error {\n    color: #dc3545;\n}\n\n/* Topic Mappings */\n.section-description {\n    font-size: 14px;\n    color: #666;\n    margin: 0 0 15px 0;\n    line-height: 1.5;\n}\n\n.section-description code {\n    background: #f5f5f5;\n    padding: 2px 6px;\n    border-radius: 3px;\n    font-family: 'Monaco', 'Courier New', monospace;\n    font-size: 13px;\n}\n\n.mapping-input {\n    display: flex;\n    gap: 10px;\n    align-items: center;\n    margin-bottom: 15px;\n}\n\n.mapping-input .mapping-source {\n    flex: 1;\n    min-width: 0;\n}\n\n.mapping-input .mapping-topic {\n    flex: 1.5;\n    min-width: 0;\n}\n\n.mapping-input .mapping-arrow {\n    color: #666;\n    font-size: 18px;\n    font-weight: bold;\n    flex-shrink: 0;\n}\n\n.mappings-list {\n    border: 1px solid #dee2e6;\n    border-radius: 4px;\n    margin: 15px 0;\n    background: #f8f9fa;\n}\n\n.mapping-item {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    padding: 12px 15px;\n    border-bottom: 1px solid #dee2e6;\n    background: white;\n    transition: background 0.2s;\n}\n\n.mapping-item:last-child {\n    border-bottom: none;\n}\n\n.mapping-item:hover {\n    background: #f8f9fa;\n}\n\n.mapping-item.disabled {\n    opacity: 0.5;\n    background: #f5f5f5;\n}\n\n.mapping-details {\n    display: flex;\n    align-items: center;\n    gap: 12px;\n    flex: 1;\n    font-family: 'Monaco', 'Courier New', monospace;\n    font-size: 13px;\n}\n\n.mapping-source-display {\n    color: #0066cc;\n    font-weight: 500;\n}\n\n.mapping-topic-display {\n    color: #28a745;\n    font-weight: 500;\n}\n\n.mapping-actions {\n    display: flex;\n    align-items: center;\n    gap: 10px;\n}\n\n.toggle-switch {\n    position: relative;\n    display: inline-block;\n    width: 44px;\n    height: 24px;\n    margin: 0;\n}\n\n.toggle-switch input {\n    opacity: 0;\n    width: 0;\n    height: 0;\n}\n\n.toggle-slider {\n    position: absolute;\n    cursor: pointer;\n    top: 0;\n    left: 0;\n    right: 0;\n    bottom: 0;\n    background-color: #ccc;\n    transition: 0.3s;\n    border-radius: 24px;\n}\n\n.toggle-slider:before {\n    position: absolute;\n    content: \"\";\n    height: 18px;\n    width: 18px;\n    left: 3px;\n    bottom: 3px;\n    background-color: white;\n    transition: 0.3s;\n    border-radius: 50%;\n}\n\n.toggle-switch input:checked + .toggle-slider {\n    background-color: #28a745;\n}\n\n.toggle-switch input:checked + .toggle-slider:before {\n    transform: translateX(20px);\n}\n\n.btn-remove-small {\n    background: none;\n    border: none;\n    color: #dc3545;\n    font-size: 20px;\n    line-height: 1;\n    cursor: pointer;\n    padding: 0 5px;\n    transition: color 0.2s;\n}\n\n.btn-remove-small:hover {\n    color: #c82333;\n}\n\n.no-mappings {\n    padding: 20px;\n    text-align: center;\n    color: #666;\n    background: #f8f9fa;\n    border: 1px dashed #dee2e6;\n    border-radius: 4px;\n    margin: 10px 0;\n}\n\n.no-mappings code {\n    background: white;\n    padding: 3px 8px;\n    border-radius: 3px;\n    font-family: 'Monaco', 'Courier New', monospace;\n    color: #0066cc;\n}\n\n/* Active Subscriptions View */\n.subscriptions-view {\n    margin-top: 30px;\n}\n\n.subscriptions-header {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    margin-bottom: 15px;\n}\n\n.subscriptions-count {\n    color: #666;\n    font-size: 14px;\n}\n\n.btn-refresh {\n    padding: 6px 12px;\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 4px;\n    cursor: pointer;\n    font-size: 13px;\n    transition: all 0.2s;\n}\n\n.btn-refresh:hover {\n    background: #e9ecef;\n    border-color: #adb5bd;\n}\n\n.subscriptions-table {\n    width: 100%;\n    border-collapse: collapse;\n    background: white;\n    border: 1px solid #dee2e6;\n    border-radius: 4px;\n    overflow: hidden;\n}\n\n.subscriptions-table th {\n    background: #f8f9fa;\n    padding: 12px;\n    text-align: left;\n    font-weight: 600;\n    color: #495057;\n    border-bottom: 2px solid #dee2e6;\n    font-size: 13px;\n}\n\n.subscriptions-table td {\n    padding: 10px 12px;\n    border-bottom: 1px solid #dee2e6;\n    font-size: 13px;\n}\n\n.subscriptions-table tr:last-child td {\n    border-bottom: none;\n}\n\n.subscriptions-table tr:hover {\n    background: #f8f9fa;\n}\n\n.tag-path {\n    font-family: 'Monaco', 'Courier New', monospace;\n    color: #0066cc;\n}\n\n.mqtt-topic {\n    font-family: 'Monaco', 'Courier New', monospace;\n    color: #28a745;\n    font-size: 12px;\n}\n\n.publish-count {\n    text-align: right;\n    color: #666;\n}\n\n.last-published {\n    color: #999;\n    font-size: 12px;\n}\n\n.quality-good {\n    color: #28a745;\n    font-weight: 500;\n}\n\n.quality-bad {\n    color: #dc3545;\n    font-weight: 500;\n}\n\n.quality-uncertain {\n    color: #ffc107;\n    font-weight: 500;\n}\n\n/* Multi-Broker Settings Layout */\n.broker-settings.multi-broker {\n    display: grid;\n    grid-template-columns: 300px 1fr;\n    gap: 20px;\n    height: calc(100vh - 250px);\n    min-height: 600px;\n}\n\n.broker-list-panel {\n    background: #f8f9fa;\n    border: 1px solid #dee2e6;\n    border-radius: 6px;\n    display: flex;\n    flex-direction: column;\n    overflow: hidden;\n}\n\n.broker-list-panel .panel-header {\n    padding: 15px;\n    border-bottom: 1px solid #dee2e6;\n    background: white;\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n}\n\n.broker-list-panel .panel-header h2 {\n    margin: 0;\n    font-size: 16px;\n    font-weight: 600;\n}\n\n.broker-list {\n    flex: 1;\n    overflow-y: auto;\n    padding: 10px;\n}\n\n.broker-item {\n    background: white;\n    border: 2px solid #dee2e6;\n    border-radius: 6px;\n    padding: 12px;\n    margin-bottom: 8px;\n    cursor: pointer;\n    transition: all 0.2s;\n}\n\n.broker-item:hover {\n    border-color: #007bff;\n    box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n}\n\n.broker-item.selected {\n    border-color: #007bff;\n    background: #e7f3ff;\n}\n\n.broker-item-header {\n    display: flex;\n    justify-content: space-between;\n    align-items: center;\n    margin-bottom: 6px;\n}\n\n.broker-item-header strong {\n    font-size: 14px;\n    color: #333;\n}\n\n.broker-item-url {\n    font-size: 12px;\n    color: #666;\n    font-family: 'Monaco', 'Courier New', monospace;\n}\n\n.badge {\n    font-size: 10px;\n    padding: 2px 8px;\n    border-radius: 10px;\n    font-weight: 600;\n    text-transform: uppercase;\n}\n\n.badge-success {\n    background: #d4edda;\n    color: #155724;\n}\n\n.broker-edit-panel {\n    background: white;\n    border: 1px solid #dee2e6;\n    border-radius: 6px;\n    padding: 20px;\n    overflow-y: auto;\n}\n\n.broker-edit-panel .panel-header {\n    margin: -20px -20px 20px -20px;\n    padding: 15px 20px;\n    border-bottom: 1px solid #dee2e6;\n    background: #f8f9fa;\n}\n\n.broker-edit-panel .panel-header h2 {\n    margin: 0;\n    font-size: 18px;\n    font-weight: 600;\n}\n\n.broker-edit-panel .empty-state {\n    text-align: center;\n    color: #999;\n    padding: 60px 20px;\n}\n\n.btn-small {\n    font-size: 12px;\n    padding: 6px 12px;\n}\n\n.form-actions .left-actions {\n    flex: 1;\n}\n\n.form-actions .right-actions {\n    display: flex;\n    gap: 10px;\n}\n\n.btn-danger {\n    background: #dc3545;\n    color: white;\n    border: 1px solid #dc3545;\n}\n\n.btn-danger:hover {\n    background: #c82333;\n    border-color: #bd2130;\n}\n\n/* Topic Mappings by Broker */\n.mapping-input-container {\n    display: flex;\n    flex-direction: column;\n    gap: 10px;\n}\n\n.mapping-input-row {\n    display: flex;\n    gap: 10px;\n    align-items: center;\n}\n\n.broker-select {\n    flex: 1;\n    padding: 8px;\n    border: 1px solid #ced4da;\n    border-radius: 4px;\n    font-size: 14px;\n}\n\n.mappings-by-broker {\n    margin-top: 20px;\n}\n\n.broker-mappings-group {\n    margin-bottom: 30px;\n}\n\n.broker-mappings-group.unassigned-group {\n    background: #fff3cd;\n    border: 2px solid #ffc107;\n    border-radius: 8px;\n    padding: 15px;\n}\n\n.broker-mappings-group.unassigned-group .broker-group-header {\n    border-bottom-color: #ffc107;\n    color: #856404;\n}\n\n.warning-text {\n    font-size: 13px;\n    color: #856404;\n    margin: 0 0 12px 0;\n    font-style: italic;\n}\n\n.broker-group-header {\n    display: flex;\n    align-items: center;\n    gap: 12px;\n    margin-bottom: 12px;\n    padding-bottom: 8px;\n    border-bottom: 2px solid #007bff;\n    font-size: 16px;\n    color: #333;\n}\n\n.broker-group-header .broker-url {\n    font-size: 13px;\n    color: #666;\n    font-family: 'Monaco', 'Courier New', monospace;\n    font-weight: normal;\n}\n\n.broker-group-header .mapping-count {\n    font-size: 12px;\n    color: #999;\n    font-weight: normal;\n    margin-left: auto;\n}\n\n.warning-message {\n    background: #fff3cd;\n    border: 1px solid #ffc107;\n    border-radius: 4px;\n    padding: 12px;\n    color: #856404;\n    margin-bottom: 15px;\n}\n\n.warning-message strong {\n    display: block;\n    margin-bottom: 4px;\n}\n"],"sourceRoot":""}]);
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

/***/ 554
(module) {

module.exports = __WEBPACK_EXTERNAL_MODULE__554__;

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

// EXTERNAL MODULE: external "http://localhost:8088/res/sys/js/react.js"
var react_js_ = __webpack_require__(341);
var react_js_default = /*#__PURE__*/__webpack_require__.n(react_js_);
// EXTERNAL MODULE: external "http://localhost:8088/res/sys/js/react-dom.js"
var react_dom_js_ = __webpack_require__(554);
var react_dom_js_default = /*#__PURE__*/__webpack_require__.n(react_dom_js_);
;// ./src/api.ts
/**
 * API client for MQTT UNS Publisher configuration and status endpoints
 */
// Data routes use the mount path alias (from getMountPathAlias)
const API_BASE = '/data/mqtt-uns-publisher';
/**
 * Generic fetch wrapper with error handling
 */
async function apiFetch(url, options) {
    try {
        console.log('[API] Fetching:', url);
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers
            }
        });
        console.log('[API] Response status:', response.status, response.statusText);
        console.log('[API] Content-Type:', response.headers.get('content-type'));
        // Get the raw text first to see what we're dealing with
        const text = await response.text();
        console.log('[API] Raw response text:', text);
        console.log('[API] Response text length:', text.length);
        // Try to parse it
        let data;
        try {
            data = JSON.parse(text);
            console.log('[API] Parsed data:', data);
        }
        catch (parseError) {
            console.error('[API] JSON parse error:', parseError);
            console.error('[API] Failed to parse text:', text);
            throw parseError;
        }
        if (!response.ok) {
            return {
                success: false,
                error: data.error || `HTTP ${response.status}: ${response.statusText}`
            };
        }
        return data;
    }
    catch (error) {
        console.error('[API] apiFetch error:', error);
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Network error'
        };
    }
}
/**
 * Get all broker configurations
 */
async function getBrokerConfig() {
    return apiFetch(`${API_BASE}/config/broker`);
}
/**
 * Get a specific broker configuration by ID
 */
async function getBrokerById(id) {
    return apiFetch(`${API_BASE}/config/broker?id=${id}`);
}
/**
 * Save broker configuration (create new or update existing)
 */
async function saveBrokerConfig(config) {
    return apiFetch(`${API_BASE}/config/broker`, {
        method: 'POST',
        body: JSON.stringify(config)
    });
}
/**
 * Delete a broker by ID
 */
async function deleteBroker(id) {
    return apiFetch(`${API_BASE}/config/broker?id=${id}`, {
        method: 'DELETE'
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
 * Get all configuration (brokers + tags)
 */
async function getAllConfig() {
    return apiFetch(`${API_BASE}/config`);
}

;// ./src/components/BrokerSettings.tsx


const BrokerSettings = ({ onBrokersChanged }) => {
    const [brokers, setBrokers] = (0,react_js_.useState)([]);
    const [selectedBrokerId, setSelectedBrokerId] = (0,react_js_.useState)(null);
    const [editingBroker, setEditingBroker] = (0,react_js_.useState)(null);
    const [isAddingNew, setIsAddingNew] = (0,react_js_.useState)(false);
    const [loading, setLoading] = (0,react_js_.useState)(true);
    const [saving, setSaving] = (0,react_js_.useState)(false);
    const [testing, setTesting] = (0,react_js_.useState)(false);
    const [message, setMessage] = (0,react_js_.useState)(null);
    // Load all brokers on mount
    (0,react_js_.useEffect)(() => {
        loadBrokers();
    }, []);
    const loadBrokers = async () => {
        setLoading(true);
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                // Auto-select first broker if available
                if (response.data.length > 0 && !selectedBrokerId) {
                    selectBroker(response.data[0]);
                }
            }
        }
        catch (error) {
            console.error('Failed to load brokers:', error);
        }
        finally {
            setLoading(false);
        }
    };
    const selectBroker = (broker) => {
        setSelectedBrokerId(broker.id || null);
        setEditingBroker({ ...broker });
        setIsAddingNew(false);
        setMessage(null);
    };
    const handleAddNew = () => {
        const newBroker = {
            name: 'New MQTT Broker',
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
            enabled: true
        };
        setEditingBroker(newBroker);
        setIsAddingNew(true);
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
    const handleTestConnection = async () => {
        if (!editingBroker)
            return;
        setTesting(true);
        setMessage(null);
        const testRequest = {
            brokerUrl: editingBroker.brokerUrl,
            clientId: editingBroker.clientId,
            username: editingBroker.username,
            password: editingBroker.password,
            useTls: editingBroker.useTls,
            connectionTimeout: editingBroker.connectionTimeout,
            keepAliveInterval: editingBroker.keepAliveInterval,
            cleanSession: editingBroker.cleanSession
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
                onBrokersChanged();
            }
            else {
                throw new Error(response.error || 'Failed to save broker');
            }
        }
        catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to save broker'
            });
        }
        finally {
            setSaving(false);
        }
    };
    const handleDelete = async (brokerId) => {
        if (!confirm('Are you sure you want to delete this broker? This action cannot be undone.')) {
            return;
        }
        try {
            const response = await deleteBroker(brokerId);
            if (response.success) {
                setMessage({ type: 'success', text: 'Broker deleted successfully' });
                await loadBrokers();
                setEditingBroker(null);
                setSelectedBrokerId(null);
                onBrokersChanged();
            }
            else {
                setMessage({
                    type: 'error',
                    text: response.error || 'Failed to delete broker'
                });
            }
        }
        catch (error) {
            setMessage({
                type: 'error',
                text: error instanceof Error ? error.message : 'Failed to delete broker'
            });
        }
    };
    const handleCancel = () => {
        if (selectedBrokerId) {
            const broker = brokers.find(b => b.id === selectedBrokerId);
            if (broker) {
                setEditingBroker({ ...broker });
            }
        }
        else {
            setEditingBroker(null);
            setIsAddingNew(false);
        }
        setMessage(null);
    };
    if (loading) {
        return react_js_default().createElement("div", { className: "broker-settings" }, "Loading brokers...");
    }
    return (react_js_default().createElement("div", { className: "broker-settings multi-broker" },
        react_js_default().createElement("div", { className: "broker-list-panel" },
            react_js_default().createElement("div", { className: "panel-header" },
                react_js_default().createElement("h2", null, "MQTT Brokers"),
                react_js_default().createElement("button", { type: "button", onClick: handleAddNew, className: "btn-primary btn-small" }, "+ Add Broker")),
            react_js_default().createElement("div", { className: "broker-list" }, brokers.length === 0 ? (react_js_default().createElement("div", { className: "empty-state" }, "No brokers configured. Click \"Add Broker\" to get started.")) : (brokers.map(broker => (react_js_default().createElement("div", { key: broker.id, className: `broker-item ${selectedBrokerId === broker.id ? 'selected' : ''}`, onClick: () => selectBroker(broker) },
                react_js_default().createElement("div", { className: "broker-item-header" },
                    react_js_default().createElement("strong", null, broker.name),
                    broker.enabled && react_js_default().createElement("span", { className: "badge badge-success" }, "Enabled")),
                react_js_default().createElement("div", { className: "broker-item-url" }, broker.brokerUrl))))))),
        react_js_default().createElement("div", { className: "broker-edit-panel" }, editingBroker ? (react_js_default().createElement("form", { onSubmit: handleSave },
            react_js_default().createElement("div", { className: "panel-header" },
                react_js_default().createElement("h2", null, isAddingNew ? 'Add New Broker' : 'Edit Broker')),
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", { htmlFor: "name" }, "Broker Name"),
                    react_js_default().createElement("input", { type: "text", id: "name", name: "name", value: editingBroker.name, onChange: handleChange, placeholder: "e.g., Production MQTT, Development Broker", required: true }),
                    react_js_default().createElement("small", null, "Friendly name to identify this broker"))),
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("h3", null, "Connection Settings"),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", { htmlFor: "brokerUrl" }, "Broker URL"),
                    react_js_default().createElement("input", { type: "text", id: "brokerUrl", name: "brokerUrl", value: editingBroker.brokerUrl, onChange: handleChange, placeholder: "tcp://localhost:1883", required: true }),
                    react_js_default().createElement("small", null, "Format: tcp://hostname:port or ssl://hostname:port")),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", { htmlFor: "clientId" }, "Client ID"),
                    react_js_default().createElement("input", { type: "text", id: "clientId", name: "clientId", value: editingBroker.clientId, onChange: handleChange, placeholder: "ignition-mqtt-publisher", required: true })),
                react_js_default().createElement("div", { className: "form-row" },
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", { htmlFor: "username" }, "Username (optional)"),
                        react_js_default().createElement("input", { type: "text", id: "username", name: "username", value: editingBroker.username || '', onChange: handleChange, placeholder: "Leave empty for anonymous" })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", { htmlFor: "password" }, "Password (optional)"),
                        react_js_default().createElement("input", { type: "password", id: "password", name: "password", value: editingBroker.password || '', onChange: handleChange, placeholder: "Leave empty for no password" })))),
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("h3", null, "MQTT Settings"),
                react_js_default().createElement("div", { className: "form-row" },
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", { htmlFor: "qos" }, "Quality of Service (QoS)"),
                        react_js_default().createElement("select", { id: "qos", name: "qos", value: editingBroker.qos, onChange: handleChange },
                            react_js_default().createElement("option", { value: 0 }, "0 - At most once"),
                            react_js_default().createElement("option", { value: 1 }, "1 - At least once"),
                            react_js_default().createElement("option", { value: 2 }, "2 - Exactly once"))),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", { htmlFor: "connectionTimeout" }, "Connection Timeout (seconds)"),
                        react_js_default().createElement("input", { type: "number", id: "connectionTimeout", name: "connectionTimeout", value: editingBroker.connectionTimeout, onChange: handleChange, min: 5, max: 300 })),
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("label", { htmlFor: "keepAliveInterval" }, "Keep Alive Interval (seconds)"),
                        react_js_default().createElement("input", { type: "number", id: "keepAliveInterval", name: "keepAliveInterval", value: editingBroker.keepAliveInterval, onChange: handleChange, min: 10, max: 3600 }))),
                react_js_default().createElement("div", { className: "form-row" },
                    react_js_default().createElement("div", { className: "form-group checkbox" },
                        react_js_default().createElement("label", null,
                            react_js_default().createElement("input", { type: "checkbox", name: "useTls", checked: editingBroker.useTls, onChange: handleChange }),
                            "Use TLS/SSL encryption")),
                    react_js_default().createElement("div", { className: "form-group checkbox" },
                        react_js_default().createElement("label", null,
                            react_js_default().createElement("input", { type: "checkbox", name: "retained", checked: editingBroker.retained, onChange: handleChange }),
                            "Retain messages on broker")),
                    react_js_default().createElement("div", { className: "form-group checkbox" },
                        react_js_default().createElement("label", null,
                            react_js_default().createElement("input", { type: "checkbox", name: "cleanSession", checked: editingBroker.cleanSession, onChange: handleChange }),
                            "Clean session on connect")),
                    react_js_default().createElement("div", { className: "form-group checkbox" },
                        react_js_default().createElement("label", null,
                            react_js_default().createElement("input", { type: "checkbox", name: "enabled", checked: editingBroker.enabled, onChange: handleChange }),
                            "Enabled")))),
            message && (react_js_default().createElement("div", { className: `message ${message.type}` }, message.text)),
            react_js_default().createElement("div", { className: "form-actions" },
                react_js_default().createElement("div", { className: "left-actions" }, !isAddingNew && editingBroker.id && (react_js_default().createElement("button", { type: "button", onClick: () => handleDelete(editingBroker.id), className: "btn-danger" }, "Delete Broker"))),
                react_js_default().createElement("div", { className: "right-actions" },
                    react_js_default().createElement("button", { type: "button", onClick: handleTestConnection, disabled: testing || saving, className: "btn-secondary" }, testing ? 'Testing...' : 'Test Connection'),
                    react_js_default().createElement("button", { type: "button", onClick: handleCancel, disabled: saving || testing, className: "btn-secondary" }, "Cancel"),
                    react_js_default().createElement("button", { type: "submit", disabled: saving || testing, className: "btn-primary" }, saving ? 'Saving...' : 'Save Broker'))))) : (react_js_default().createElement("div", { className: "empty-state" }, "Select a broker from the list or add a new one.")))));
};
/* harmony default export */ const components_BrokerSettings = (BrokerSettings);

;// ./src/components/TagSelection.tsx


const TagSelection = ({ config, onConfigSaved }) => {
    const [formData, setFormData] = (0,react_js_.useState)({
        name: 'Default Tag Publishing',
        enabled: false,
        tagProviders: ['default'],
        tagFolders: [],
        topicMappings: [],
        topicOverrides: {},
        payloadTemplate: '',
        includeMetadata: true,
        valueDeadband: 0.1,
        publishOnQualityChange: true
    });
    const [brokers, setBrokers] = (0,react_js_.useState)([]);
    const [selectedBrokerId, setSelectedBrokerId] = (0,react_js_.useState)(null);
    const [saving, setSaving] = (0,react_js_.useState)(false);
    const [message, setMessage] = (0,react_js_.useState)(null);
    const [newMappingSource, setNewMappingSource] = (0,react_js_.useState)('');
    const [newMappingTopic, setNewMappingTopic] = (0,react_js_.useState)('');
    const [newMappingBrokerId, setNewMappingBrokerId] = (0,react_js_.useState)(null);
    (0,react_js_.useEffect)(() => {
        if (config) {
            setFormData(config);
        }
        // Always reload brokers when component mounts or config changes
        loadBrokers();
    }, [config]);
    // Also reload brokers when tab becomes visible (e.g., after adding a broker)
    (0,react_js_.useEffect)(() => {
        const handleFocus = () => {
            loadBrokers();
        };
        window.addEventListener('focus', handleFocus);
        return () => window.removeEventListener('focus', handleFocus);
    }, []);
    const loadBrokers = async () => {
        try {
            const response = await getBrokerConfig();
            if (response.success && response.data) {
                setBrokers(response.data);
                // Auto-select first broker for new mappings
                if (response.data.length > 0 && !newMappingBrokerId) {
                    setNewMappingBrokerId(response.data[0].id || null);
                }
            }
        }
        catch (error) {
            console.error('Failed to load brokers:', error);
        }
    };
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
    const addTopicMapping = () => {
        if (newMappingSource && newMappingTopic && newMappingBrokerId) {
            const newMapping = {
                id: Date.now().toString(),
                brokerId: newMappingBrokerId,
                sourcePattern: newMappingSource,
                topicPrefix: newMappingTopic,
                enabled: true
            };
            setFormData(prev => ({
                ...prev,
                topicMappings: [...prev.topicMappings, newMapping]
            }));
            setNewMappingSource('');
            setNewMappingTopic('');
            // Keep the same broker selected for convenience
        }
        else {
            setMessage({
                type: 'error',
                text: 'Please fill in all fields and select a broker'
            });
        }
    };
    const removeTopicMapping = (id) => {
        setFormData(prev => ({
            ...prev,
            topicMappings: prev.topicMappings.filter(m => m.id !== id)
        }));
    };
    const toggleMappingEnabled = (id) => {
        setFormData(prev => ({
            ...prev,
            topicMappings: prev.topicMappings.map(m => m.id === id ? { ...m, enabled: !m.enabled } : m)
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
    // Group mappings by broker
    const mappingsByBroker = formData.topicMappings.reduce((acc, mapping) => {
        const key = mapping.brokerId ?? 'unassigned';
        if (!acc[key]) {
            acc[key] = [];
        }
        acc[key].push(mapping);
        return acc;
    }, {});
    const getBrokerName = (brokerId) => {
        if (brokerId === 'unassigned') {
            return 'Unassigned (No Broker)';
        }
        const broker = brokers.find(b => b.id === brokerId);
        return broker ? broker.name : `Broker ${brokerId}`;
    };
    return (react_js_default().createElement("div", { className: "tag-selection" },
        react_js_default().createElement("form", { onSubmit: handleSave },
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("h2", null, "UNS Topic Mappings"),
                react_js_default().createElement("p", { className: "section-description" },
                    "Map tag providers or folders to custom UNS topic prefixes and assign them to MQTT brokers. Only tags matching enabled mappings will be published to their assigned broker. Example: Map ",
                    react_js_default().createElement("code", null, "[Sample_Tags]Random"),
                    " to ",
                    react_js_default().createElement("code", null, "enterprise/site1/line1")),
                brokers.length === 0 ? (react_js_default().createElement("div", { className: "warning-message" },
                    react_js_default().createElement("strong", null, "No brokers configured."),
                    " Please configure at least one MQTT broker in the Broker Settings tab before creating topic mappings.",
                    react_js_default().createElement("button", { type: "button", onClick: loadBrokers, className: "btn-secondary", style: { marginLeft: '10px' } }, "Refresh"))) : (react_js_default().createElement((react_js_default()).Fragment, null,
                    react_js_default().createElement("div", { className: "form-group" },
                        react_js_default().createElement("div", { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' } },
                            react_js_default().createElement("label", null, "Add New Topic Mapping"),
                            react_js_default().createElement("button", { type: "button", onClick: loadBrokers, className: "btn-secondary", style: { fontSize: '12px', padding: '4px 10px' } }, "\uD83D\uDD04 Refresh Brokers")),
                        react_js_default().createElement("div", { className: "mapping-input-container" },
                            react_js_default().createElement("div", { className: "mapping-input-row" },
                                react_js_default().createElement("select", { value: newMappingBrokerId || '', onChange: (e) => setNewMappingBrokerId(Number(e.target.value)), className: "broker-select" },
                                    react_js_default().createElement("option", { value: "" }, "Select broker..."),
                                    brokers.map(broker => (react_js_default().createElement("option", { key: broker.id, value: broker.id },
                                        broker.name,
                                        " (",
                                        broker.brokerUrl,
                                        ")"))))),
                            react_js_default().createElement("div", { className: "mapping-input-row" },
                                react_js_default().createElement("input", { type: "text", value: newMappingSource, onChange: (e) => setNewMappingSource(e.target.value), placeholder: "Source pattern (e.g., [default]TestTags)", className: "mapping-source" }),
                                react_js_default().createElement("span", { className: "mapping-arrow" }, "\u2192"),
                                react_js_default().createElement("input", { type: "text", value: newMappingTopic, onChange: (e) => setNewMappingTopic(e.target.value), placeholder: "UNS topic prefix (e.g., enterprise/site1/area2)", className: "mapping-topic" }),
                                react_js_default().createElement("button", { type: "button", onClick: addTopicMapping, className: "btn-add" }, "Add Mapping")))),
                    formData.topicMappings.length > 0 ? (react_js_default().createElement("div", { className: "mappings-by-broker" }, selectedBrokerId === null ? (
                    // Show all brokers (and unassigned)
                    react_js_default().createElement((react_js_default()).Fragment, null,
                        mappingsByBroker['unassigned'] && mappingsByBroker['unassigned'].length > 0 && (react_js_default().createElement("div", { key: "unassigned", className: "broker-mappings-group unassigned-group" },
                            react_js_default().createElement("h3", { className: "broker-group-header" },
                                "\u26A0\uFE0F Unassigned Mappings (No Broker)",
                                react_js_default().createElement("span", { className: "mapping-count" },
                                    mappingsByBroker['unassigned'].length,
                                    " mapping",
                                    mappingsByBroker['unassigned'].length !== 1 ? 's' : '')),
                            react_js_default().createElement("p", { className: "warning-text" }, "These mappings were created before multi-broker support. Delete them and recreate with a broker assigned."),
                            react_js_default().createElement("div", { className: "mappings-list" }, mappingsByBroker['unassigned'].map(mapping => (react_js_default().createElement("div", { key: mapping.id, className: "mapping-item disabled" },
                                react_js_default().createElement("div", { className: "mapping-details" },
                                    react_js_default().createElement("span", { className: "mapping-source-display" }, mapping.sourcePattern),
                                    react_js_default().createElement("span", { className: "mapping-arrow" }, "\u2192"),
                                    react_js_default().createElement("span", { className: "mapping-topic-display" }, mapping.topicPrefix)),
                                react_js_default().createElement("div", { className: "mapping-actions" },
                                    react_js_default().createElement("button", { type: "button", onClick: () => removeTopicMapping(mapping.id), className: "btn-remove-small", title: "Delete this unassigned mapping" }, "\u2715 Delete")))))))),
                        brokers.map(broker => {
                            const brokerMappings = mappingsByBroker[broker.id] || [];
                            if (brokerMappings.length === 0)
                                return null;
                            return (react_js_default().createElement("div", { key: broker.id, className: "broker-mappings-group" },
                                react_js_default().createElement("h3", { className: "broker-group-header" },
                                    broker.name,
                                    react_js_default().createElement("span", { className: "broker-url" }, broker.brokerUrl),
                                    react_js_default().createElement("span", { className: "mapping-count" },
                                        brokerMappings.length,
                                        " mapping",
                                        brokerMappings.length !== 1 ? 's' : '')),
                                react_js_default().createElement("div", { className: "mappings-list" }, brokerMappings.map(mapping => (react_js_default().createElement("div", { key: mapping.id, className: `mapping-item ${!mapping.enabled ? 'disabled' : ''}` },
                                    react_js_default().createElement("div", { className: "mapping-details" },
                                        react_js_default().createElement("span", { className: "mapping-source-display" }, mapping.sourcePattern),
                                        react_js_default().createElement("span", { className: "mapping-arrow" }, "\u2192"),
                                        react_js_default().createElement("span", { className: "mapping-topic-display" }, mapping.topicPrefix)),
                                    react_js_default().createElement("div", { className: "mapping-actions" },
                                        react_js_default().createElement("label", { className: "toggle-switch" },
                                            react_js_default().createElement("input", { type: "checkbox", checked: mapping.enabled, onChange: () => toggleMappingEnabled(mapping.id) }),
                                            react_js_default().createElement("span", { className: "toggle-slider" })),
                                        react_js_default().createElement("button", { type: "button", onClick: () => removeTopicMapping(mapping.id), className: "btn-remove-small" }, "\u2715"))))))));
                        }))) : (
                    // Show selected broker only
                    react_js_default().createElement("div", { className: "broker-mappings-group" },
                        react_js_default().createElement("h3", null, getBrokerName(selectedBrokerId)),
                        react_js_default().createElement("div", { className: "mappings-list" }, (mappingsByBroker[selectedBrokerId] || []).map(mapping => (react_js_default().createElement("div", { key: mapping.id, className: `mapping-item ${!mapping.enabled ? 'disabled' : ''}` },
                            react_js_default().createElement("div", { className: "mapping-details" },
                                react_js_default().createElement("span", { className: "mapping-source-display" }, mapping.sourcePattern),
                                react_js_default().createElement("span", { className: "mapping-arrow" }, "\u2192"),
                                react_js_default().createElement("span", { className: "mapping-topic-display" }, mapping.topicPrefix)),
                            react_js_default().createElement("div", { className: "mapping-actions" },
                                react_js_default().createElement("label", { className: "toggle-switch" },
                                    react_js_default().createElement("input", { type: "checkbox", checked: mapping.enabled, onChange: () => toggleMappingEnabled(mapping.id) }),
                                    react_js_default().createElement("span", { className: "toggle-slider" })),
                                react_js_default().createElement("button", { type: "button", onClick: () => removeTopicMapping(mapping.id), className: "btn-remove-small" }, "\u2715")))))))))) : (react_js_default().createElement("p", { className: "no-mappings" }, "No custom topic mappings configured yet.")),
                    react_js_default().createElement("small", null, "Custom mappings override default topic generation for matching tags")))),
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("h2", null, "Publishing Settings"),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", { htmlFor: "valueDeadband" }, "Value Deadband"),
                    react_js_default().createElement("input", { type: "number", id: "valueDeadband", name: "valueDeadband", value: formData.valueDeadband, onChange: handleChange, min: 0, step: 0.01 }),
                    react_js_default().createElement("small", null, "Minimum change required to publish (prevents noise from small fluctuations)")),
                react_js_default().createElement("div", { className: "form-group" },
                    react_js_default().createElement("label", { htmlFor: "payloadTemplate" }, "Payload Template (optional)"),
                    react_js_default().createElement("textarea", { id: "payloadTemplate", name: "payloadTemplate", value: formData.payloadTemplate || '', onChange: handleChange, placeholder: "Leave empty for default JSON format", rows: 4 }),
                    react_js_default().createElement("small", null, "Custom JSON template for message payload (advanced users only)")),
                react_js_default().createElement("div", { className: "form-group checkbox" },
                    react_js_default().createElement("label", null,
                        react_js_default().createElement("input", { type: "checkbox", name: "includeMetadata", checked: formData.includeMetadata, onChange: handleChange }),
                        "Include metadata (timestamp, quality, datatype)")),
                react_js_default().createElement("div", { className: "form-group checkbox" },
                    react_js_default().createElement("label", null,
                        react_js_default().createElement("input", { type: "checkbox", name: "publishOnQualityChange", checked: formData.publishOnQualityChange, onChange: handleChange }),
                        "Publish when tag quality changes"))),
            react_js_default().createElement("div", { className: "form-section" },
                react_js_default().createElement("h2", null, "Module Settings"),
                react_js_default().createElement("div", { className: "form-group checkbox" },
                    react_js_default().createElement("label", null,
                        react_js_default().createElement("input", { type: "checkbox", name: "enabled", checked: formData.enabled, onChange: handleChange }),
                        react_js_default().createElement("strong", null, "Enable tag publishing")),
                    react_js_default().createElement("small", null, "Tag publishing must be enabled to monitor and publish tag changes"))),
            message && (react_js_default().createElement("div", { className: `message ${message.type}` }, message.text)),
            react_js_default().createElement("div", { className: "form-actions" },
                react_js_default().createElement("button", { type: "submit", disabled: saving, className: "btn-primary" }, saving ? 'Saving...' : 'Save Configuration')))));
};
/* harmony default export */ const components_TagSelection = (TagSelection);

;// ./src/components/StatusDashboard.tsx


const StatusDashboard = () => {
    const [status, setStatus] = (0,react_js_.useState)(null);
    const [loading, setLoading] = (0,react_js_.useState)(true);
    const [error, setError] = (0,react_js_.useState)(null);
    const [autoRefresh, setAutoRefresh] = (0,react_js_.useState)(true);
    (0,react_js_.useEffect)(() => {
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
                console.log('Status response:', response.data);
                console.log('Statistics:', response.data.statistics);
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
        return (react_js_default().createElement("div", { className: "status-dashboard" },
            react_js_default().createElement("div", { className: "loading" }, "Loading status...")));
    }
    if (error) {
        return (react_js_default().createElement("div", { className: "status-dashboard" },
            react_js_default().createElement("div", { className: "error-banner" },
                react_js_default().createElement("strong", null, "Error:"),
                " ",
                error,
                react_js_default().createElement("button", { onClick: loadStatus, className: "btn-retry" }, "Retry"))));
    }
    if (!status) {
        return null;
    }
    return (react_js_default().createElement("div", { className: "status-dashboard" },
        react_js_default().createElement("div", { className: "status-header" },
            react_js_default().createElement("h2", null, "Module Status"),
            react_js_default().createElement("label", { className: "auto-refresh" },
                react_js_default().createElement("input", { type: "checkbox", checked: autoRefresh, onChange: (e) => setAutoRefresh(e.target.checked) }),
                "Auto-refresh (5s)")),
        react_js_default().createElement("div", { className: "status-grid" },
            react_js_default().createElement("div", { className: "status-card" },
                react_js_default().createElement("h3", null, "Health Status"),
                react_js_default().createElement("div", { className: "status-content" },
                    react_js_default().createElement("div", { className: `badge ${getHealthBadgeClass(status.healthLevel)}` }, status.healthLevel || 'UNKNOWN'),
                    react_js_default().createElement("p", { className: "status-message" }, status.statusMessage))),
            react_js_default().createElement("div", { className: "status-card" },
                react_js_default().createElement("h3", null, "MQTT Brokers"),
                react_js_default().createElement("div", { className: "status-content" },
                    react_js_default().createElement("div", { className: `badge ${getConnectionBadgeClass(status.connectionState)}` }, status.connectionStateDisplay || status.connectionState || 'UNKNOWN'),
                    status.totalBrokers === 0 ? (react_js_default().createElement("p", { className: "broker-counts" }, "No brokers configured")) : status.activeBrokers === 0 ? (react_js_default().createElement((react_js_default()).Fragment, null,
                        react_js_default().createElement("p", { className: "broker-counts" },
                            status.totalBrokers,
                            " available, 0 in use"),
                        react_js_default().createElement("p", { className: "hint-text" },
                            "Create topic mappings in ",
                            react_js_default().createElement("strong", null, "Tag Selection"),
                            " to activate"))) : (react_js_default().createElement("p", { className: "broker-counts" },
                        status.activeBrokers,
                        " connected / ",
                        status.totalBrokers,
                        " available")))),
            react_js_default().createElement("div", { className: "status-card" },
                react_js_default().createElement("h3", null, "Tag Monitoring"),
                react_js_default().createElement("div", { className: "status-content" },
                    react_js_default().createElement("div", { className: "stat-value" }, status.monitoredTagCount),
                    react_js_default().createElement("p", { className: "stat-label" }, "Monitored Tags"))),
            react_js_default().createElement("div", { className: "status-card" },
                react_js_default().createElement("h3", null, "Uptime"),
                react_js_default().createElement("div", { className: "status-content" },
                    react_js_default().createElement("div", { className: "stat-value" }, status.statistics?.uptimeDisplay || 'N/A'),
                    react_js_default().createElement("p", { className: "stat-label" },
                        status.statistics?.uptimeMs?.toLocaleString() || '0',
                        " ms")))),
        react_js_default().createElement("div", { className: "statistics-section" },
            react_js_default().createElement("h3", null, "Publishing Statistics"),
            react_js_default().createElement("div", { className: "stats-grid" },
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Messages Published"),
                    react_js_default().createElement("span", { className: "stat-value" }, status.statistics?.messagesPublished?.toLocaleString() || '0')),
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Messages Failed"),
                    react_js_default().createElement("span", { className: "stat-value" }, status.statistics?.messagesFailed?.toLocaleString() || '0')),
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Publish Success Rate"),
                    react_js_default().createElement("span", { className: "stat-value" },
                        status.statistics?.publishSuccessRate?.toFixed(1) || '0',
                        "%")),
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Tag Reads Successful"),
                    react_js_default().createElement("span", { className: "stat-value" }, status.statistics?.tagReadsSuccessful?.toLocaleString() || '0')),
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Tag Reads Failed"),
                    react_js_default().createElement("span", { className: "stat-value" }, status.statistics?.tagReadsFailed?.toLocaleString() || '0')),
                react_js_default().createElement("div", { className: "stat-item" },
                    react_js_default().createElement("label", null, "Tag Read Success Rate"),
                    react_js_default().createElement("span", { className: "stat-value" },
                        status.statistics?.tagReadSuccessRate?.toFixed(1) || '0',
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
    console.log('[Configuration] Component rendering started');
    const [activeTab, setActiveTab] = (0,react_js_.useState)('broker');
    const [tagConfig, setTagConfig] = (0,react_js_.useState)(null);
    const [loading, setLoading] = (0,react_js_.useState)(true);
    const [error, setError] = (0,react_js_.useState)(null);
    (0,react_js_.useEffect)(() => {
        console.log('[Configuration] useEffect triggered - loading configuration');
        loadConfiguration();
    }, []);
    const loadConfiguration = async () => {
        console.log('[Configuration] loadConfiguration called');
        setLoading(true);
        setError(null);
        try {
            // Load tag config (brokers are now loaded by BrokerSettings component)
            console.log('[Configuration] Fetching tag config...');
            const tagResponse = await getTagConfig();
            console.log('[Configuration] Tag response:', tagResponse);
            if (tagResponse.success && tagResponse.data) {
                setTagConfig(tagResponse.data);
            }
            else if (!tagResponse.success) {
                throw new Error(tagResponse.error || 'Failed to load tag configuration');
            }
        }
        catch (err) {
            console.error('[Configuration] Error loading configuration:', err);
            setError(err instanceof Error ? err.message : 'Failed to load configuration');
        }
        finally {
            setLoading(false);
            console.log('[Configuration] loadConfiguration completed');
        }
    };
    const handleBrokersChanged = () => {
        // Reload tag config when brokers change to ensure consistency
        loadConfiguration();
    };
    const handleTagConfigSaved = (newConfig) => {
        setTagConfig(newConfig);
    };
    if (loading) {
        console.log('[Configuration] Rendering loading state');
        return (react_js_default().createElement("div", { className: "mqtt-config-page" },
            react_js_default().createElement("div", { className: "loading" }, "Loading configuration...")));
    }
    if (error) {
        console.log('[Configuration] Rendering error state:', error);
        return (react_js_default().createElement("div", { className: "mqtt-config-page" },
            react_js_default().createElement("div", { className: "error-banner" },
                react_js_default().createElement("strong", null, "Error:"),
                " ",
                error,
                react_js_default().createElement("button", { onClick: loadConfiguration, className: "btn-retry" }, "Retry"))));
    }
    console.log('[Configuration] Rendering main UI, activeTab:', activeTab);
    return (react_js_default().createElement("div", { className: "mqtt-config-page" },
        react_js_default().createElement("header", { className: "page-header" },
            react_js_default().createElement("h1", null, "MQTT UNS Publisher Configuration"),
            react_js_default().createElement("p", { className: "page-description" }, "Configure MQTT broker connections and tag publishing settings")),
        react_js_default().createElement("div", { className: "tabs" },
            react_js_default().createElement("button", { className: `tab ${activeTab === 'broker' ? 'active' : ''}`, onClick: () => setActiveTab('broker') }, "Broker Settings"),
            react_js_default().createElement("button", { className: `tab ${activeTab === 'tags' ? 'active' : ''}`, onClick: () => setActiveTab('tags') }, "Tag Publishing"),
            react_js_default().createElement("button", { className: `tab ${activeTab === 'status' ? 'active' : ''}`, onClick: () => setActiveTab('status') }, "Status & Statistics")),
        react_js_default().createElement("div", { className: "tab-content" },
            activeTab === 'broker' && (react_js_default().createElement(components_BrokerSettings, { onBrokersChanged: handleBrokersChanged })),
            activeTab === 'tags' && (react_js_default().createElement(components_TagSelection, { config: tagConfig, onConfigSaved: handleTagConfigSaved })),
            activeTab === 'status' && (react_js_default().createElement(components_StatusDashboard, null)))));
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
        console.error('[ErrorBoundary] getDerivedStateFromError:', error);
        return {
            hasError: true,
            error,
            errorInfo: null
        };
    }
    componentDidCatch(error, errorInfo) {
        console.error('[ErrorBoundary] componentDidCatch - Error:', error);
        console.error('[ErrorBoundary] componentDidCatch - Error message:', error.message);
        console.error('[ErrorBoundary] componentDidCatch - Error stack:', error.stack);
        console.error('[ErrorBoundary] componentDidCatch - Component stack:', errorInfo.componentStack);
        this.setState({
            error,
            errorInfo
        });
        // Store in window for debugging
        if (typeof window !== 'undefined') {
            window.MQTT_LAST_ERROR = {
                error,
                errorInfo,
                timestamp: new Date().toISOString()
            };
            console.error('[ErrorBoundary] Error stored in window.MQTT_LAST_ERROR');
        }
    }
    render() {
        if (this.state.hasError) {
            console.error('[ErrorBoundary] Rendering error UI');
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
                        } }, this.state.errorInfo.componentStack))),
                react_js_default().createElement("p", { style: { marginTop: '10px', fontSize: '12px', color: '#666' } }, "Check console for full error details. Error also stored in window.MQTT_LAST_ERROR")));
        }
        return this.props.children;
    }
}
/* harmony default export */ const components_ErrorBoundary = (ErrorBoundary);

;// ./src/index.tsx




// IMMEDIATE debug logging - runs as soon as file is parsed
console.log('=== MQTT MODULE LOADING ===');
console.log('[MQTT Module] TOP OF FILE - Script is being parsed');
console.log('[MQTT Module] timestamp:', new Date().toISOString());
// Debug logging to help diagnose the issue
console.log('[MQTT Module] index.tsx loaded');
console.log('[MQTT Module] React available:', typeof (react_js_default()) !== 'undefined');
console.log('[MQTT Module] ReactDOM available:', typeof (react_dom_js_default()) !== 'undefined');
console.log('[MQTT Module] ConfigurationComponent type:', typeof components_Configuration);
// For Ignition Gateway SystemJS loading
// Based on logs, Ignition calls with different signatures to probe the component type
// Let's handle all cases and log what we receive
console.log('[MQTT Module] Exporting ConfigurationComponent with wrapper');
// Wrapper that handles different calling conventions
const ConfigurationWrapper = function (...args) {
    console.log('[MQTT Module] Configuration wrapper called');
    console.log('[MQTT Module] this:', this);
    console.log('[MQTT Module] args:', args);
    console.log('[MQTT Module] args.length:', args.length);
    // Check if called with 'new' keyword
    if (new.target) {
        console.log('[MQTT Module] Called with new keyword (constructor)');
        // Return an instance
        return react_js_default().createElement(components_Configuration);
    }
    // Check calling patterns
    if (args.length === 0) {
        console.log('[MQTT Module] Called with no args - returning React element');
        return react_js_default().createElement(components_Configuration);
    }
    if (args.length >= 1) {
        console.log('[MQTT Module] Called with args - creating React element with props');
        const props = args[0] && typeof args[0] === 'object' ? args[0] : {};
        console.log('[MQTT Module] Props for component:', props);
        return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration, props) });
    }
    // Check if first arg looks like a DOM element
    if (args[0] && args[0].nodeType) {
        console.log('[MQTT Module] Called with DOM element - mounting component');
        react_dom_js_default().render(react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) }), args[0]);
        return;
    }
    console.log('[MQTT Module] Fallback - returning React element');
    return react_js_default().createElement(components_ErrorBoundary, { children: react_js_default().createElement(components_Configuration) });
};
// Make it look like a React component
ConfigurationWrapper.displayName = 'Configuration';
const moduleExports = {
    Configuration: ConfigurationWrapper
};
// Log after export
console.log('[MQTT Module] Module exports:', moduleExports);
console.log('[MQTT Module] Configuration type:', typeof moduleExports.Configuration);
console.log('[MQTT Module] Configuration component:', moduleExports.Configuration);
// Try to log to window for debugging
if (typeof window !== 'undefined') {
    window.MQTT_MODULE_DEBUG = {
        loaded: true,
        timestamp: new Date().toISOString(),
        exports: moduleExports,
        React: typeof (react_js_default()),
        ReactDOM: typeof (react_dom_js_default())
    };
    console.log('[MQTT Module] Debug info stored in window.MQTT_MODULE_DEBUG');
}
console.log('=== MQTT MODULE EXPORT ===');
console.log('[MQTT Module] About to export:', moduleExports);
// Try to manually verify SystemJS registration after module loads
if (typeof window !== 'undefined') {
    setTimeout(() => {
        const System = window.System;
        if (System) {
            const registered = System.get('com.inductiveautomation.mqtt.uns.gateway');
            console.log('[MQTT Module] SystemJS registration check:', registered);
            if (!registered) {
                console.error('[MQTT Module] WARNING: Module not registered in SystemJS!');
                // Try to list all registered modules
                if (System.entries) {
                    console.log('[MQTT Module] Attempting to list all SystemJS modules:');
                    const entries = Array.from(System.entries());
                    console.log('[MQTT Module] Registered modules:', entries);
                }
                else if (System.registry) {
                    console.log('[MQTT Module] SystemJS registry:', Object.keys(System.registry));
                }
                else if (System._loader && System._loader.moduleRecords) {
                    console.log('[MQTT Module] Module records:', Object.keys(System._loader.moduleRecords));
                }
            }
            else {
                console.log('[MQTT Module] ✓ Module successfully registered!');
            }
        }
    }, 500); // Increased timeout to give SystemJS more time
}
/* harmony default export */ const src = (moduleExports);

__webpack_exports__ = __webpack_exports__["default"];
/******/ 	return __webpack_exports__;
/******/ })()
;
});
//# sourceMappingURL=mqtt-config.js.map