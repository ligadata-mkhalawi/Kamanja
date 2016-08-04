'use strict';
angular.module('networkApp')
  .service('serviceConfig', [function () {
    return {
      upperLimitToInValues: 2000,
      classImageColorPath: "./images/",
      classImageColorMap: {
        model: {
          image: 'model',
          extension:'svg',
          iconColor: '#96f',
          headerColor: '#93f',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        message: {
          image: 'message',
          extension:'svg',
          iconColor: '#fc9',
          headerColor: '#f93',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        input: {
          image: 'input',
          extension:'svg',
          iconColor: '#cc6',
          headerColor: '#c93',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        output: {
          image: 'output',
          extension:'svg',
          iconColor: '#3ff',
          headerColor: '#6cc',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        storage: {
          image: 'storage',
          extension:'svg',
          iconColor: '#cf9',
          headerColor: '#9c3',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        container: {
          image: 'container',
          extension:'svg',
          iconColor: '#909',
          headerColor: '#f6f',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        system: {
          image: 'system',
          extension:'svg',
          iconColor: '#cc3',
          headerColor: '#663',
          width: '45',
          height: '45',
          widthProperties: '25',
          heightProperties: '25'
        },
        dag: {
          image: 'dag',
          extension:'svg',
          iconColor: '#fc9',
          headerColor: '#f93',
          width: '25',
          height: '25',
          widthProperties: '25',
          heightProperties: '25'
        }
      }
    };
  }]);
