/**
 * Created by muhammad on 8/29/16.
 */

'use strict';
angular.module('flareApp')
  .directive('nodeDetails', ['$rootScope', function($rootScope){
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/nodeDetails.html',
      controllerAs: 'nodeDetails',
      controller: function($rootScope){
        var nodeDetails = this;

        $rootScope.$on('nodeClicked', function(event, data){
          toggleNodeDetails(true);
        });

      }
    }
  }]);
