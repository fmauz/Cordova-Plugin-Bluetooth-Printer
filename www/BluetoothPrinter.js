var exec = require('cordova/exec');

var BTPrinter = {
   list: function(fnSuccess, fnError){
      exec(fnSuccess, fnError, "BluetoothPrinter", "list", []);
   },
   connect: function(fnSuccess, fnError, name){
      exec(fnSuccess, fnError, "BluetoothPrinter", "connect", [name]);
   },
   disconnect: function(fnSuccess, fnError){
      exec(fnSuccess, fnError, "BluetoothPrinter", "disconnect", []);
   },
   print: function(fnSuccess, fnError, str){
      exec(fnSuccess, fnError, "BluetoothPrinter", "print", [str]);
   },
   printText: function(fnSuccess, fnError, str){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printText", [str]);
   },
   printPoule: function(fnSuccess, fnError, poule, paper_type){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printPoule", [poule,paper_type]);
   },
   printResult: function(fnSuccess, fnError, poule, paper_type){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printResult", [poule,paper_type]);
   },
   printReport: function(fnSuccess, fnError, poule, paper_type){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printReport", [poule,paper_type]);
   },
   printCanceled: function(fnSuccess, fnError, poule, paper_type){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printCanceled", [poule,paper_type]);
   },
   printPayment: function(fnSuccess, fnError, poule, paper_type){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printPayment", [poule,paper_type]);
   },
   printImage: function(fnSuccess, fnError, str){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printImage", [str]);
   },
   printPOSCommand: function(fnSuccess, fnError, str){
      exec(fnSuccess, fnError, "BluetoothPrinter", "printPOSCommand", [str]);
   }
};

module.exports = BTPrinter;
