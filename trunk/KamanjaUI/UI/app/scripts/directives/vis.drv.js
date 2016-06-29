/*globals angular,_,vis, console, document */
angular
  .module('networkApp')
  .directive('uiHomeVisDemo', ['serviceConfig', '$timeout', '$window', function (serviceConfig, $timeout, $window) {
    'use strict';
    return {
      restrict: 'E',
      scope: {
        showStatus: '@',
        data: '<',
        viewName: '@',
        nodeClick: '<',
        nodeDoubleClick: '<',
        edgeClick: '<',
        groundClick: '<'
      },
      template: '<div><div id="visJsDiv"></div>' ,
    // +
    //     '<div class="checkbox" style="position: absolute; bottom:97px;right:100px; color: white">' +
    //     '<label>' +
    //     '<input type="checkbox" ng-model="disablePhysicsStatus" ng-change="disablePhysicsChange()" > disable physics' +
    //     '</label>' +
    //     '</div>' +
    //     '</div>',
      replace: true,
      link: function (scope) {
        var isNodeActive, updateNodesImagesToBeInactive, updateNodeToBeActive;
        var container = document.getElementById('visJsDiv');
        var data = {nodes: new vis.DataSet([]), edges: new vis.DataSet([])};
        var options = {
          'edges': {
            'smooth': {
              'type': 'curvedCCW',
              'roundness': 0.35
            },
            shadow: {
              enabled: true,
              color: 'rgba(0,0,0,1)',
              size: 15,
              x: 0,
              y: 0
            },
            arrows: {
              from: {enabled: false, scaleFactor: 0.5}
            }
          },

          'physics': {
            'barnesHut': {
              'gravitationalConstant': -10000,
              'centralGravity': 0.5,
              'springLength': 75,
              'springConstant': 0.09,
              'damping': 0.5,
              'avoidOverlap': 1
            },
            'minVelocity': 0.5,
            'solver': 'barnesHut',
            // 'timestep': 0.79
          },
          // 'physics': {
          //   'forceAtlas2Based': {
          //     'centralGravity': 0.005,
          //     'springLength': 60,
          //     'springConstant': 0.09,
          //     'gravitationalConstant': -80,
          //     'damping': 0.88,
          //     'avoidOverlap': 0.53
          //   },
          //   'minVelocity': 0.6,
          //   'solver': 'forceAtlas2Based',
          //   'timestep': 0.79
          // },
          layout: {
            randomSeed: 2
          },
          nodes: {
            shape: 'dot',
            size: 30,
            font: {
              size: 32
            },
            borderWidth: 0,
            shadow: true
          },
          interaction: {
            hover: true,
            navigationButtons: true,
            keyboard: true
          }
        };

        var network = new vis.Network(container, data, options);

        var Node = function (n) {
          var imagePath = serviceConfig.classImageColorPath;
          var types = serviceConfig.classImageColorMap;
          this.id = n.id;
          this.number = n.number;
          this._label = n.name || '';
          this.shape = n.shape || 'image';
          this.size = n.size || 16;
          this.type = types[n['class']];
          this.image = imagePath + this.type.image + '.inactive.' + this.type.extension;
          this.active = false;
        };

        var Edge = function (e) {
          this.id = e.id;
          this.label = e.label;
          this.from = e.from;
          this.to = e.to;
          this.toNode = data.nodes.getItemById(this.to);
          this.arrows = 'from';

          var color = scope.viewName.toLowerCase() === 'dag' ? '#f93' : this.toNode.type.headerColor;

          this.color = color;
          this.font = {
            size: 8,
            color: 'white',
            strokeWidth: 0,
            align: 'middle',
            background: 'rgba(0,0,0,1)',
          };
          if (e.duplicates >= 2) {
            this.smooth = {
              type: 'dynamic'
            };
          }
        };

        (function fixNodeEdgePointerHover() {
          network.on('hoverNode', function () {
            angular.element('html,body').css('cursor', 'pointer');
          });
          network.on('blurNode', function () {
            angular.element('html,body').css('cursor', 'default');
          });
          network.on('hoverEdge', function () {
            angular.element('html,body').css('cursor', 'pointer');
          });
          network.on('blurEdge', function () {
            angular.element('html,body').css('cursor', 'default');
          });
        }());
        (function clickDoubleClickFix() {
          var doubleClickTime = 0;
          var threshold = 200;

          function doOnClick(params) {
            var id = params.nodes[0];
            if (id) {
              scope.nodeClick(id);
            } else {
              id = params.edges[0];
              if (id) {
                updateNodesImagesToBeInactive();
                scope.edgeClick(id);
              }
            }
          }

          function doOnDoubleClick(params) {
            var id = params.nodes[0];
            if (id) {
              scope.nodeDoubleClick(id);
            }
          }

          function onClick(params) {
            var t0 = new Date();
            if (t0 - doubleClickTime > threshold) {
              $timeout(function () {
                if (t0 - doubleClickTime > threshold) {
                  doOnClick(params);
                }
              }, threshold);
            }
          }

          function onDoubleClick(params) {
            doubleClickTime = new Date();
            doOnDoubleClick(params);
          }

          network.on('click', onClick);
          network.on('doubleClick', onDoubleClick);
        }());
        network.on('click', function (params) {
          if (!params.edges.length && !params.nodes.length) {
            updateNodesImagesToBeInactive();
            scope.groundClick();
          }
        });
        (function imageManipulationFunctions() {
          isNodeActive = function (id) {
            var node = data.nodes.getItemById(id);
            return node.image.match(/\.active\./);
          };
          updateNodesImagesToBeInactive = function () {
            data.nodes.forEach(function (n) {
              data.nodes.update({
                id: n.id,
                image: serviceConfig.classImageColorPath + n.type.image + '.inactive.' + n.type.extension
              });
            });
          };
          updateNodeToBeActive = function (id) {
            var node = data.nodes.getItemById(id);
            data.nodes.update({
              id: id,
              image: serviceConfig.classImageColorPath + node.type.image + '.active.' + node.type.extension
            });
          };
        }());
        scope.$on('closeSideMenu', function () {
          updateNodesImagesToBeInactive();
        });
        network.on('click', function (params) {
          var id = params.nodes[0];
          if (id) {
            var alreadyActive = isNodeActive(id);
            updateNodesImagesToBeInactive();
            if (!alreadyActive) {
              updateNodeToBeActive(id);
            }
          }
        });
        network.on('afterDrawing', function (ctx) {
          data.nodes.forEach(function (d) {
            if (d.hidden) {
              return;
            }
            var position = network.getPositions(d.id)[d.id];

            if (scope.showStatus === 'true') {

              ctx.textAlign = 'left';
              ctx.font = '9px arial';
              ctx.fillStyle = '#ffffff';
              ctx.fillText(d._label, (position.x + 13), (position.y + 10));
              ctx.font = '9px arial';
              ctx.fillStyle = '#FFCC00';
              ctx.fillText(d.number, (position.x + 13), (position.y + 10));
            } else {

              var rectWidth = d._label.length * 6 + 10;
              var rectHeight = 10;
              var cornerRadius = 6;

              ctx.lineJoin = "round";
              ctx.lineWidth = 10;
              ctx.strokeStyle = "rgba(0,0,0,0.5)";
              ctx.strokeRect((position.x + 14), (position.y - 3), rectWidth - cornerRadius, rectHeight - cornerRadius);

              ctx.textAlign = 'left';
              ctx.font = '9px arial';
              ctx.fillStyle = '#ffffff';
              ctx.fillText(d._label, (position.x + 13), (position.y - 3 ));
            }

          });
        });
        var resizeNetworkAndReposition = function () {
          var windowHeight = $window.outerHeight;
          if (windowHeight >= 768) {
            var height = windowHeight.toString() + 'px';
            network.setOptions({height: height});
            container.style.height = height;
          }
          var windowWidth = $window.innerWidth;
          if (windowWidth >= 1024) {
            var width = windowWidth.toString() + 'px';
            network.setOptions({width: width});
            container.style.width = width;
          }
          network.moveTo(
            {
              position: {x: 0, y: 0},
              scale: 1.5
            }
          );
        };
        $window.onresize = resizeNetworkAndReposition;
        scope.$watch('data', function () {
          data.nodes.removeAll();
          data.nodes.add((function () {
            var nodes = [];
            _.each(scope.data.nodes, function (n) {
              nodes.push(new Node(n));
            });
            return nodes;
          }()));
          data.edges.removeAll();
          data.edges.add((function () {
            var edges = [];
            _.each(scope.data.edges, function (e) {
              edges.push(new Edge(e));
            });
            return edges;
          }()));
          resizeNetworkAndReposition();
        });
        scope.disablePhysicsChange = function () {
          if (scope.disablePhysicsStatus) {
            network.setOptions({physics: false});
          } else {
            network.setOptions({physics: options});
          }
        };
      }
    };
  }]);
(function () {
  vis.DataSet.prototype.getItemById = function (id) {
    'use strict';
    return this.get({
      filter: function (item) {
        return item.id === id;
      }
    })[0];
  };
  vis.DataSet.prototype.removeAll = function () {
    'use strict';
    var self = this;
    this.forEach(function (d) {
      self.remove(d.id);
    });
  };
// vis.DataSet.prototype.updateAll = function (updateObject) {
//   'use strict';
//   var self = this;
//   this.forEach(function (d) {
//     updateObject.id = d.id;
//     self.update(updateObject);
//   });
// };
}());
