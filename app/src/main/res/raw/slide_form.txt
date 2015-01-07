var Forms = {
  listen: function() {
    $(".form li input").click(function(evt) {
      evt.preventDefault();
    });
    $(".form li").click(function(evt) {
      if( $(this).hasClass("active") ) {
        $(this).removeClass("active");
      } else {
        $(this).siblings().removeClass("active");
        $(this).addClass("active");
      }
    });
    $(".form > li.select li").click(function(evt) {
      var $input = $(this).parents(".select").children("input");
      if( $(this).hasClass("other") ) {
        $input.prop("readonly", false);
        $input.focus();
      } else {
        $input.val($(this).text());
        $input.prop("readonly", true);
      }
    });
  },
  createField: function(field) {
    var className = "text";
    if( field.type == "select" ) {
      className = "select";
    }
    var item = $("<li></li>", {
      "class": className,
      "data-field": field.name
    });
    if( className == "text" ) {
      var input = $("<input>", {
        "type": field.type,
        "placeholder": "...",
        "autocorrect": "off",
        "autocapitalize": "off"
      });
      item.append(input);
    } else if( className == "select" ) {
      var input = $("<input>", {
        "type": field.subType,
        "value": field.options[0],
        "readonly": true,
        "autocorrect": "off",
        "autocapitalize": "off"
      });
      var options = $("<ul>", {
        "class": "options"
      });
      field.options.forEach(function(option) {
        var optionElm = $("<li>");
        optionElm.text(option);
        options.append(optionElm);
      });
      if( field.custom ) {
        var otherElm = $("<li>", {
          "class": "other"
        });
        otherElm.text("Other");
        options.append(otherElm);
      }
      item.append(input);
      item.append(options);
    }
    return item;
  },
  createForm: function(container, fields) {
    fields.forEach(function(field) {
      $(field).appendTo(container);
    });
  },
  textField: function(name) {
    return Forms.createField({ type: "text", name: name });
  },
  numberField: function(name) {
    return Forms.createField({ type: "number", name: name });
  },
  selectField: function(name, options, custom) {
    return Forms.createField({
      type: "select",
      subType: "text",
      name: name,
      options: options,
      custom: custom
    });
  },
  populateForm: function(fields) {
    $(function() {
      Forms.createForm($(".form"), fields);
      Forms.listen();
    });
  },
  serializeForm: function() {
    var fields = $(".form").children("li");
    var keystore = {};
    [].map.call(fields, function(field) {
      var key = $(field).data("field"),
          value = $(field).find("input").val();
      keystore[key] = value;
    });
    return JSON.stringify(keystore);
  }
};

