/**
 * Created by muhammad on 8/29/16.
 */

'use strict';
angular.module('flareApp')
  .directive('nodeDetails', ['$rootScope', 'serviceData', function($rootScope, serviceData){
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/nodeDetails.html',
      controllerAs: 'nodeDetails',
      controller: function($rootScope){
        var nodeDetails = this;

        $rootScope.$on('nodeClicked', function(event, data){
          if(data) {
            toggleNodeDetails(true);
            nodeDetails.headerColor = data.group.color.background;
            nodeDetails.applicationName = data.node.id;
            serviceData.getDummyData(data.node.id, function (result) {
              //nodeDetails.result = result[0];
            });
          }else{
            toggleNodeDetails(false);
          }
        });

      }
    }
  }]);
