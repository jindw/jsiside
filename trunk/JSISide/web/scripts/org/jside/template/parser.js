/*
 * JavaScript Integration Framework
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/jsi/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
//parse
var EL_TYPE = 0;
var VAR_TYPE = 1;//":set"://var
var IF_TYPE = 2;//":if":
var ELSE_TYPE = 3;//":else":
var FOR_TYPE = 4;//":for":

var EL_TYPE_XML_TEXT = 6;
var ATTRIBUTE_TYPE = 7;//":attribute":


var FOR_KEY = "this["+FOR_TYPE+"]";
var TEMPLATE_NS_REG = /^http:\/\/www.xidea.org\/ns\/template.*/;


//add as default
function Parser(){
    this.parserList = this.parserList.concat([]);
    this.result = [];
}


/**
 * @private
 */
Parser.prototype = {
    parserList : [],
    /**
     * 添加新解析函数
     * @public
     */
    addParser : function(){
        this.parserList.push.apply(this.parserList,arguments)
    },
    /**
     * 想当前栈顶添加数据
     * 解析和编译过程中使用
     * @public
     */
    append  :  function(){
        var result = this.result;
        for(var i = 0;i<arguments.length;i++){
            var item = arguments[i];
            //alert(result)
            if(result.length){
                if(item.constructor == String){
                    var previous = result.pop();
                    if(previous.constructor == String){
                        result.push(previous+item);
                    }else{
                        result.push(previous,item);  
                    }
                }else{
                    result.push(item);
                }
            }else{
                result.push(item);
            }
        }
        //alert(result)
    },
    /**
     * 移除结尾数据直到上一个end为止（包括该end标记）
     * @public
     */
    removeLastEnd:function(){
        var result = this.result;
        var item;
        while((item = result.pop())!=null){
            if(item instanceof Array && item.length ==0){//end
                break;
            }
        }
    },
    /**
     * 给出文件内容或url，解析模版源文件。
     * 如果指定了base，当作url解析，无base，当作纯文本解析
     * @public
     * @abstract
     * @return <Array> result
     */
    parse : function(node){
        throw new Error("未实现")
    },
    /**
     * 解析源文件文档节点。
     * @public 
     */
    parseNode : function(node){
        var parserList = this.parserList;
        var i = parserList.length;
        while(i--){
            if(parserList[i](node,this)){
                break;
            }
        }
    }
}