package org.jside.template;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class Template {
	public static final int EL_TYPE_XML_TEXT = 6;
	public static final int ATTRIBUTE_TYPE = 7;
	public static final int EL_TYPE = 0;
	public static final int ELSE_TYPE = 3;
	public static final int FOR_TYPE = 4;
	public static final int IF_TYPE = 2;
	public static final int VAR_TYPE = 1;

	public static final String FOR_KEY = "_[4]";

	//private static ExpressionFactory propertyExpressionFactory = new PropertyExpressionFactory();

	private List<Object> items;

	public Template(List<Object> list) {
		this.items = this.compile(list);
	}

	public Template(Object source, Parser parser) {
		this(parser.parse(source));
	}

	public void render(Object context, Writer out)
			throws IOException {
		renderList(new ContextWrapper(context), items,out);
	}

	protected void renderList(Map<Object, Object> context,
			List<Object> children,Writer out) {
		for (Object item : children) {
			try {
				if (item instanceof TemplateItem) {
					((TemplateItem) item).render(context,out);
				} else {
					out.write((String) item);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 编译模板数据,这里没有递归调用
	 * 
	 * @internal
	 */
	private ArrayList<Object> compile(List<Object> datas) {
		ArrayList<ArrayList<Object>> itemsStack = new ArrayList<ArrayList<Object>>();
		itemsStack.add(new ArrayList<Object>());
		for (int i = 0; i < datas.size(); i++) {
			Object item = datas.get(i);
			// alert(typeof item)
			if (item instanceof String) {
				itemsStack.get(0).add(item);
			} else {
				// alert(typeof item)
				compileItem((Object[]) item, itemsStack);
			}
		}
		return itemsStack.get(0);
	}

	/**
	 * 模板单元编译函数
	 * 
	 * @internal
	 */
	private void compileItem(Object[] data,
			ArrayList<ArrayList<Object>> itemsStack) {

		if (data.length == 0) {
			itemsStack.remove(itemsStack.size() - 1);
			return;
		}
		switch ((Integer) data[0]) {
		case EL_TYPE:// ":el":
			buildExpression(data, itemsStack, false);
			break;
		case EL_TYPE_XML_TEXT:// ":el":
			buildExpression(data, itemsStack, true);
			break;
		case VAR_TYPE:// ":set"://var
			buildVar(data, itemsStack);
			break;
		case IF_TYPE:// ":if":
			buildIf(data, itemsStack);
			break;
		case ELSE_TYPE:// ":else-if":":else":
			buildElse(data, itemsStack);
			break;
		case FOR_TYPE:// ":for":
			buildFor(data, itemsStack);
			break;
		case ATTRIBUTE_TYPE:// ":attribute":
			buildAttribute(data, itemsStack);
			break;
		}
	}

	public static class ForStatus {
		int index = 0;
		final int end;

		public ForStatus(int end) {
			this.end = end;
		}

		public int getIndex() {
			return index;
		}

		public int getEnd() {
			return end;
		}

	}

	private static interface TemplateItem {
		public void render(Map<Object, Object> context,Writer out)
				throws IOException;
	}

	public Expression createExpression(final Object el) {
		if (el instanceof String) {
			return new PropertyExpression(new String[] { (String) el });
		} else if (el instanceof Object[]) {
			return new PropertyExpression((Object[]) el);
		} else if (el instanceof Expression) {
			return (Expression) el;
		}
		return new Expression() {
			public Object evaluate(Object context) {
				return el;
			}
		};
	}

	protected void printXMLText(String text, Map<Object, Object> context,Writer out, char quteChar)
			throws IOException {
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
				break;
			case '>':
				out.write("&gt;");
				break;
			case '&':
				out.write("&amp;");
				break;
			case '\'':
				if (quteChar == c) {
					out.write("&#39;");
				}
				break;
			case '"':
				if (quteChar == c) {
					out.write("&#34;");
				}
				break;
			default:
				out.write(c);
			}
		}
	}

	private void pushToTop(ArrayList<ArrayList<Object>> itemsStack,
			TemplateItem item) {
		itemsStack.get(itemsStack.size() - 1).add(item);
	}

	protected boolean toBoolean(Object test) {
		return test != null && !Boolean.FALSE.equals(test) && !"".equals(test);
	}

	private void buildExpression(Object[] data,
			ArrayList<ArrayList<Object>> itemsStack, final boolean encodeXML) {
		final Expression el = createExpression(data[1]);
		pushToTop(itemsStack, new TemplateItem() {
			public void render(Map<Object, Object>  context,Writer out)
					throws IOException {
				Object value = el.evaluate(context);
				if (encodeXML && value != null) {
					printXMLText(String.valueOf(value), context, out,(char) 0);
				} else {
					out.write(String.valueOf(value));
				}
			}
		});
	}

	private void buildIf(Object[] data, ArrayList<ArrayList<Object>> itemsStack) {
		final Expression el = createExpression(data[1]);
		final ArrayList<Object> children = new ArrayList<Object>();
		pushToTop(itemsStack, new TemplateItem() {
			public void render(Map<Object, Object>  context,Writer out) {
				boolean test = toBoolean(el.evaluate(context));
				if (test) {
					renderList(context, children, out);
				}
				// context[2] = test;//if passed(一定要放下来，确保覆盖)
				context.put(IF_TYPE, test);
			}

		});
		itemsStack.add(children);
	}

	private void buildElse(Object[] data,
			ArrayList<ArrayList<Object>> itemsStack) {
		// TODO:why???
		// itemsStack.shift();???
		itemsStack.remove(itemsStack.size() - 1);//
		final Expression el = data[1] == null ? null
				: createExpression(data[1]);
		final ArrayList<Object> children = new ArrayList<Object>();
		pushToTop(itemsStack, new TemplateItem() {
			public void render(Map<Object, Object>  context,Writer out) {
				if (!toBoolean(context.get(IF_TYPE))) {
					if (el == null || toBoolean(el.evaluate(context))) {// if
						renderList(context, children, out);
						context.put(IF_TYPE, true);
						;// if passed(不用要放下去，另一分支已正常)
					}
				}
			}
		});
		itemsStack.add(children);
	}

	private void buildFor(Object[] data, ArrayList<ArrayList<Object>> itemsStack) {
		final String varName = (String) data[1];
		final Expression itemExpression = createExpression(data[2]);
		final String statusName = (String) data[3];
		final ArrayList<Object> children = new ArrayList<Object>();
		pushToTop(itemsStack, new TemplateItem() {
			public void render(Map<Object, Object>  context,Writer out) {
				Object list = itemExpression.evaluate(context);
				List<Object> items;
				// alert(data.constructor)
				if (list instanceof Object[]) {
					items = Arrays.asList(list);
				} else {
					items = (List<Object>) list;
				}
				ForStatus preiousStatus = (ForStatus) context.get(FOR_KEY);
				int i = 0;
				int len = items.size();
				ForStatus forStatus = new ForStatus(len - 1);
				context.put(FOR_KEY, forStatus);
				// prepareFor(this);
				if (statusName != null) {
					context.put(statusName, forStatus);
				}
				for (Object item : items) {
					forStatus.index++;
					context.put(varName, item);
					renderList(context, children, out);
				}
				if (statusName != null) {
					context.put(statusName, preiousStatus);
				}
				context.put(statusName, preiousStatus);// for key
				context.put(IF_TYPE, len > 0);// if key
			}
		});
		itemsStack.add(children);
	}

	private void buildVar(Object[] data, ArrayList<ArrayList<Object>> itemsStack) {
		final String name = (String) data[1];
		if (data[2] != null) {
			final Expression el = createExpression(data[2]);
			pushToTop(itemsStack, new TemplateItem() {
				public void render(Map<Object, Object>  context,Writer out) {
					context.put(name, el.evaluate(context));
				}
			});
		} else {
			final ArrayList<Object> children = new ArrayList<Object>();
			pushToTop(itemsStack, new TemplateItem() {
				public void render(Map<Object, Object>  context,Writer out) {
					StringWriter buf = new StringWriter();
					renderList(context, children,buf);
					context.put(name, buf.toString());
				}
			});
			itemsStack.add(children);// #end
		}
	}

	private void buildAttribute(Object[] data,
			ArrayList<ArrayList<Object>> itemsStack) {
		final String prefix = " " + data[1] + "=\"";
		final Expression el = createExpression(data[2]);
		pushToTop(itemsStack, new TemplateItem() {
			public void render(Map<Object, Object>  context,Writer out)
					throws IOException {
				Object value = el.evaluate(context);
				if (value != null) {
					printXMLText(prefix, context, out, (char) 0);
					printXMLText(String.valueOf(value), context, out, '\"');
					out.write('"');
				}
			}
		});
	}
}
