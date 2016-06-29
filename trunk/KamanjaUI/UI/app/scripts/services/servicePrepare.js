/*globals angular, _ */
angular.module('networkApp')
  .service('servicePrepare', [function () {
    'use strict';
    return {
      viewToVis: function (viewData) {
        var edges = _.filter(viewData.result, function (d) {
          return d.Type !== "V";
        });
        var occurrence = {};

        _.each(edges, function (e) {
          e.id = e['@rid'];
          e.from = e['in'];
          e.label = e.Name;
          e.to = e.out;
          e.duplicates = 1;
          occurrence[e.from + ',' + e.to] = (occurrence[e.from + ',' + e.to] || 0) + 1;
          if (occurrence[e.from + ',' + e.to] > 1){
            _.each(edges, function (ee) {
              if (ee.from === e.from && ee.to === e.to){
                ee.duplicates = occurrence[e.from + ',' + e.to];
              }
            });
          }
        });
        
        var nodes = _.filter(viewData.result, function (d) {
          return d.Type === "V";
        });
        _.each(nodes, function (n) {
          n.ID = null;
          n.id = n['@rid'];
          n.name = n.Name;
          n['class'] = n['@class'].toLowerCase();
        });
        return {nodes: nodes, edges: edges};
      }
    };
  }]);
