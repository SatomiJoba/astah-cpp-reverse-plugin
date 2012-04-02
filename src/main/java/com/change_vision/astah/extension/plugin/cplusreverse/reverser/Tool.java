package com.change_vision.astah.extension.plugin.cplusreverse.reverser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ModelEditorFactory;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IClassifierTemplateParameter;
import com.change_vision.jude.api.inf.model.IGeneralization;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IOperation;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.model.IParameter;
import com.change_vision.jude.api.inf.model.ITemplateBinding;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectAccessorFactory;

public class Tool {
	
	public static List GlobalList = new ArrayList();
	
	/**
	 * get package from the path, if not exits, will create one.
	 * @param path: the full path of the package
	 * @return IPackage of the path
	 */
	public static IPackage getPackage(String[] path) throws ProjectNotFoundException,
			ClassNotFoundException, InvalidEditingException {
		IModel project = ProjectAccessorFactory.getProjectAccessor()
				.getProject();
		IPackage result = project;
		for (int i = 0; i < path.length; i++) {
			result = getPackage(result, path[i]);
		}
		return result;
	}

	/**
	 * get package under parent, if not exits, will create one,
	 * @param parent: the parent package
	 * @param path: the name of the package
	 * @return the package under the parent, and the name is path
	 */
	static IPackage getPackage(IPackage parent, String path)
			throws ProjectNotFoundException, ClassNotFoundException,
			InvalidEditingException {
		INamedElement[] array = parent.getOwnedElements();
		for (int i = 0; i < array.length; i++) {
			if (array[i].getName().equals(path) && array[i] instanceof IPackage) {
				return (IPackage) array[i];
			}
		}
		return ModelEditorFactory.getBasicModelEditor().createPackage(parent,
				path);
	}
	
	/**
	 * get class named className under the allPath
	 * @param allPath: the class's package's path
	 * @param className: the class's name
	 * @return the class named className under the allpath 
	 */
	public static IClass getClass(String[] allPath, String className) throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		IClass anonimousClass = getAnonimousClass(allPath, null, className, new ArrayList<Ref>());
		if (anonimousClass != null) {
			return anonimousClass;
		}
		IPackage iPkg = getPackage(allPath);
		INamedElement[] elms = iPkg.getOwnedElements();
		for (int i = 0; i < elms.length; i++) {
			if (elms[i].getName().equals(className) && elms[i] instanceof IClass) {
				return (IClass) elms[i];
			}
		}
		return ModelEditorFactory.getBasicModelEditor().createClass(iPkg, className);
	}

	/**
	 * get the InnerClass named className under parentclass 
	 * @param parentclass
	 * @param className
	 * @return the innerClass under parentclass
	 */
	public static IClass getClass(IClass parentclass, String className) throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		IClass anonimousClass = getAnonimousClass(parentclass, null, className, new ArrayList<Ref>());
		if (anonimousClass != null) {
			return anonimousClass;
		}
		IClass nestClasses = getNestedClass(parentclass, className);
		if (nestClasses != null) {
			return nestClasses;
		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createClass(parentclass, className));
	}
	
	public static IClass getNestedClass(IClass parentclass, String className) {
		IClass[] nestClasses = parentclass.getNestedClasses();
		for (int i = 0; i < nestClasses.length; i++) {
			if (nestClasses[i].getName().equals(className)) {
				return nestClasses[i];
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param parentPkg: the class's package
	 * @param className: the class's name
	 * @return the Class under parentPkg, which named className.
	 */
	public static IClass getClass(IPackage parentPkg, String className) throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		IClass anonimousClass = getAnonimousClass(parentPkg, null, className, new ArrayList<Ref>());
		if (anonimousClass != null) {
			return anonimousClass;
		}
		INamedElement[] namedElements = parentPkg.getOwnedElements();
		for (int i = 0; i < namedElements.length; i++) {
			if (namedElements[i].getName().equals(className) && namedElements[i] instanceof IClass) {
				if(GlobalList.contains((IClass)namedElements[i])){
					return changeGlobalName(parentPkg, className, namedElements, i);
				}
				return (IClass) namedElements[i];
			}
		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createClass(parentPkg, className));
	}
	public static IClass getClass(String type, List<Ref> typeRefs) throws InvalidEditingException, ClassNotFoundException, ProjectNotFoundException {
		if (typeRefs.isEmpty()) {
			return null;
		}
		IClass templateClass = (IClass) CompoundDef.compounddef.get(typeRefs.get(0).getRefid());
		if (templateClass == null) {
			return null;
		}
		if (type.indexOf("<") == -1) {
			return templateClass;
		}
		String paramString = type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
		String[] params = paramString.split(",");
		
		Object[][] actualClasses = new Object[params.length][];
		for (int i = 0, index = 1; i < params.length; i++) {
			Object actual = null;
			String paramTrim = params[i].trim();
			Object[] filterKeyword = filterKeyword(paramTrim);
			if (!filterKeyword[1].equals("")) {
				actual = filterKeyword[1];
				//TODO: allocator<T>
//				//allocator<>
//				if (params[i].indexOf("<") != -1 && params[i].indexOf(">") != -1) {
//					String subParamString = paramTrim.substring(paramTrim.indexOf("<") + 1, paramTrim.indexOf(">")).trim();
//					String[] ss = subParamString.split(",");
//					StringBuilder sb = new StringBuilder(paramTrim.substring(0, paramTrim.indexOf("<") + 1));
//					for (int j = 0; j < ss.length; j++) {
//						if (!ss[j].trim().equals("")) {
//							sb.append(ss[j].trim());
//						} else {
//							IClass cls = (IClass) CompoundDef.compounddef.get(typeRefs.get(index++).getRefid());
//							sb.append(cls.getName());
//						}
//						if (j != ss.length - 1) {
//							sb.append(",");
//						}
//					}
//					sb.append(">");
//					actual = sb.toString();
//				}
			} else if (index < typeRefs.size()
					&& CompoundDef.compounddef.containsKey(typeRefs.get(index).getRefid())) {
				actual = CompoundDef.compounddef.get(typeRefs.get(index++).getRefid());
			}
			actualClasses[i] = new Object[] {actual, filterKeyword[0]};
		}
		//find existed anonymous bound class
		ITemplateBinding[] tBindings = templateClass.getTemplateBindings();
		for (int i = 0; i < tBindings.length; i++) {
			ITemplateBinding tBinding = tBindings[i];
			IClass boundClass = tBinding.getBoundElement();
			if (boundClass.getName().equals("") && matchesAllActualValues(tBinding, actualClasses)) {
				return boundClass;
			}
		}
		//create anonymous bound class
		BasicModelEditor basicModelEditor = ModelEditorFactory.getBasicModelEditor();
		IClassifierTemplateParameter[] tParams = templateClass.getTemplateParameters();
		if (tParams.length < actualClasses.length) {
			IClass paraType = null;
			for (int i = 0; i < actualClasses.length - tParams.length; i++) {
				basicModelEditor.createTemplateParameter(templateClass, "param" + i, paraType, null);
			}
		}
		tParams = templateClass.getTemplateParameters();
		ProjectAccessor prjAccessor = ProjectAccessorFactory.getProjectAccessor();
		IModel project = prjAccessor.getProject();
		String tempName = getUniqueName(project);
		IClass anonimousClass = basicModelEditor.createClass(project, tempName);
		ITemplateBinding binding = basicModelEditor.createTemplateBinding(anonimousClass, templateClass);
		for (int i = 0; i < actualClasses.length; i++) {
			IClassifierTemplateParameter param = tParams[i];
			binding.addActualParameter(param, actualClasses[i][0]);
			if (!"".equals(actualClasses[i][1])) {
				binding.setActualParameterTypeModifier(param, actualClasses[i][1].toString());
			}
		}			
		anonimousClass.setName("");
		return anonimousClass;
	}
	
	private static String getUniqueName(IPackage parent) {
		String initName = "anonymousboundclass";
		Set names = new HashSet();
		for (Object obj : parent.getOwnedElements()) {
			if (obj instanceof INamedElement) {
				names.add(((INamedElement) obj).getName());
			}
		}
		for (int i = 0; ; i++) {
			String name = initName + i;
			if (!names.contains(name)) {
				return name;
			}
		}
	}

	/**
	 * @param parentPkg
	 * @param className
	 * @param namedElements
	 * @param i
	 * @return
	 * @throws ProjectNotFoundException
	 * @throws ClassNotFoundException
	 * @throws InvalidEditingException
	 */
	private static IClass changeGlobalName(IPackage parentPkg, String className, INamedElement[] namedElements, int i)
			throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		int index = 0;
		String name = "Global";
		IClass globalClass = null;
		while (index < 100) {
			globalClass = Tool.getGlobalClass(parentPkg, name);
			if(globalClass==null){
				break;
			}
			name = "Global" + "_" + index++;	
		}
		for(int j=0;j<GlobalList.size();j++){
			if((IClass)namedElements[i] ==(IClass)GlobalList.get(j)){
				((IClass)GlobalList.get(j)).setName(name);
			}
		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createClass(parentPkg, className));
	}
	
	/**
	 * 
	 * @param subClass
	 * @param superClass
	 * @return the Generalization between subClass and superClass
	 */
	public static IGeneralization getGeneralization(IClass subClass, IClass superClass) throws InvalidEditingException, ClassNotFoundException {
		IGeneralization[] generlations = subClass.getGeneralizations();
		for (int i = 0; i < generlations.length; i++) {
			if (generlations[i].getSuperType().equals(superClass))
				return generlations[i];
		}
		return ModelEditorFactory.getBasicModelEditor().createGeneralization(subClass, superClass, "");
	}
	
	/**
	 * 
	 * @param target: the attribute in the class
	 * @param name: the attribute's name
	 * @param type: the attribute's type, the type is IClass
	 * @return the attribute in the target Class, which's name and type
	 */
	public static IAttribute getAttribute(IClass target, String name, IClass type) throws InvalidEditingException, ClassNotFoundException {
		IAttribute[] attrs = target.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getName().equals(name)) {
				name = changeSameAttributeName(target, name);			
			}
		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createAttribute(target, name, type));
	}
	
	/**
	 * 
	 * @param target: the attribute in the class
	 * @param name: the attribute's name
	 * @param type: the attribute's type, the type is the primitive type in jude
	 * @return the attribute in the target Class, which's name and type
	 */
	public static IAttribute getAttribute(IClass target, String name, String type) throws InvalidEditingException, ClassNotFoundException {
		IAttribute[] attrs = target.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getName().equals(name) && attrs[i].getTypeExpression().equals(type)) {
				name = changeSameAttributeName(target, name);
			}
		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createAttribute(target, name, type));
	}
	
	public static IOperation getOperation(IClass target, String name, String type) throws InvalidEditingException, ClassNotFoundException {
//		IOperation[] attrs = target.getOperations();
		//no need to judge
//		for (int i = 0; i < attrs.length; i++) {
//			if (attrs[i].getName().equals(name)
//					&& ((type == null && type == attrs[i].getReturnTypeExpression()) || type.equals(attrs[i].getReturnTypeExpression()))) {
//				return attrs[i];
//			}
//		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createOperation(target, name, type));
	}

	public static IOperation getOperation(IClass target, String name, IClass type) throws InvalidEditingException, ClassNotFoundException {
//		IOperation[] attrs = target.getOperations();
		//no need to judge
//		for (int i = 0; i < attrs.length; i++) {
//			if (attrs[i].getName().equals(name)
//					&& ((type == null && type == attrs[i].getReturnType()) || type.equals(attrs[i].getReturnType()))) {
//				return attrs[i];
//			}
//		}
		return setLanguage(ModelEditorFactory.getBasicModelEditor().createOperation(target, name, type));
	}
	
	public static IParameter getOperationParameter(IOperation operation, String name, String type) throws InvalidEditingException, ClassNotFoundException {
		IParameter[] params = operation.getParameters();
		for (int i = 0; i < params.length; i++) {
			if (params[i].getName().equals(name)
					&& type.equals(params[i].getType().getName())) {
				return params[i];
			}
		}
		return ModelEditorFactory.getBasicModelEditor().createParameter(operation, name, type);
	}
	
	//set iattribute's Language, Now Jude support C# and JAVA
	private static IAttribute setLanguage(IAttribute iattr) throws InvalidEditingException {
		if (LanguageManager.isCSHARP()) {
			String[] steretypes = iattr.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C# Attribute")) {
					return iattr;
				}
			}
			iattr.addStereotype("C# Attribute");
		} else if (LanguageManager.isJAVA()) {
			String[] steretypes = iattr.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("Java Attribute")) {
					return iattr;
				}
			}
			iattr.addStereotype("Java Attribute");
		} else if (LanguageManager.isCPlus()) {
			String[] steretypes = iattr.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C++ Attribute")) {
					return iattr;
				}
			}
			iattr.addStereotype("C++ Attribute");
		}
		return iattr;
	}

	//set iclass's Language, Now Jude support C# and JAVA
	public static IClass setLanguage(IClass iclass) throws InvalidEditingException {
		if (LanguageManager.isCSHARP()) {
			String[] steretypes = iclass.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C# Class")) {
					return iclass;
				}
			}
			iclass.addStereotype("C# Class");
		} else if (LanguageManager.isJAVA()) {
			String[] steretypes = iclass.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("Java Class")) {
					return iclass;
				}
			}
			iclass.addStereotype("Java Class");
		} else if (LanguageManager.isCPlus()) {
			String[] steretypes = iclass.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C++ Class")) {
					return iclass;
				}
			}
			iclass.addStereotype("C++ Class");
		}
		return iclass;
	}

	//set ioperation's Language, Now Jude support C# and JAVA
	private static IOperation setLanguage(IOperation oper) throws InvalidEditingException {
		if (LanguageManager.isCSHARP()) {
			String[] steretypes = oper.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C# Method")) {
					return oper;
				}
			}
			oper.addStereotype("C# Method");
		} else if (LanguageManager.isJAVA()) {
			String[] steretypes = oper.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("Java Method")) {
					return oper;
				}
			}
			oper.addStereotype("Java Method");
		} else if (LanguageManager.isCPlus()) {
			String[] steretypes = oper.getStereotypes();
			for (int i = 0; i < steretypes.length; i++) {
				if (steretypes[i].equals("C++ Method")) {
					return oper;
				}
			}
			oper.addStereotype("C++ Method");
		}
		return oper;
	}
	
	/**
	 * 
	 * @param parentPkg: the Global class's package
	 * @param className: the Global class's name
	 * @return the Global Class under parentPkg, which named className.
	 */
	public static IClass getGlobalClass(IPackage parentPkg, String className) throws ProjectNotFoundException,
			ClassNotFoundException, InvalidEditingException {
		INamedElement[] namedElements = parentPkg.getOwnedElements();
		for (int i = 0; i < namedElements.length; i++) {
			if (namedElements[i].getName().equals(className) && namedElements[i] instanceof IClass) {
				return (IClass) namedElements[i];
			}
		}
		return null;
//		IClass iClass = setLanguage(ModelEditorFactory.getBasicModelEditor().createClass(parentPkg, className));
//		GlobalList.add(iClass);
//		return iClass;
	}
	
	private static IAttribute getSameAttribute(IClass target, String name) throws InvalidEditingException, ClassNotFoundException {
		IAttribute[] attrs = target.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getName().equals(name)) {
				return attrs[i];		
			}
		}
		return null;
	}
	/**
	 * @param target
	 * @param name
	 * @param type
	 * @return
	 * @throws InvalidEditingException
	 * @throws ClassNotFoundException
	 */
	private static String changeSameAttributeName(IClass target, String name)
			throws InvalidEditingException, ClassNotFoundException {
		int index = 0;
		IAttribute iAttribute = null;
		while (index < 100) {
			name = name + "_" + index++;	
			iAttribute = getSameAttribute(target, name);
			if(iAttribute==null){
				break;
			}
		}
		return name;
	}
	
	public static IClass getAnonimousClass(Object parent, String[] nameSpace, String templateString, List<Ref> typeDefs)
	throws InvalidEditingException, ProjectNotFoundException, ClassNotFoundException {
		IClass templateClass = null;
		int beginIndex = templateString.indexOf("<");
		int endIndex = templateString.lastIndexOf(">");
		if (!typeDefs.isEmpty()) {
			templateClass = (IClass) CompoundDef.compounddef.get(typeDefs.get(0).getRefid());
			if (templateClass != null && templateClass.getTemplateParameters().length == 0) {
				templateClass = null;
			}
		}

		if (templateClass == null) {
			if (beginIndex == -1 || endIndex == -1) {
				return null;
			}
			String head = templateString.substring(0, beginIndex);
			head = filterInvalidChar(head);
			
			if ("".equals(head)) {
				return null;
			}		
			String[] path = head.split("::");
			if (path.length > 1) {
				String[] namespace2 = new String[path.length - 1];
				System.arraycopy(path, 0, namespace2, 0, path.length - 1);
				templateClass = Tool.getClass(namespace2, path[path.length - 1]);
			}
			if (templateClass == null) {
				if (nameSpace != null) {
					templateClass = getClass(nameSpace, head);
				} else if (parent instanceof IPackage) {
					templateClass = getClass((IPackage) parent, head);
				} else if (parent instanceof IClass) {
					templateClass = getClass((IClass) parent, head);
				} else if (parent instanceof String[]) {
					templateClass = getClass((String[]) parent, head);
				}
			}
		}
		if (templateClass == null) {
			return null;
		}
		
		//Doxygen1.5.8, type="TemplateClass<actualStr, actualStr2>", typeDefs={}
		//Doxygen1.7.0:
		//TemplateClass<actualClass>,   type="<>", typeDefs={ref(TemplateClass), ref(actualClass)}
		//TemplateClass<actualClass*>,   type="<*>", typeDefs={ref(TemplateClass), ref(actualClass)}
		//TemplateClass<int>,           type="<int>", typeDefs={ref(TemplateClass)}
		//TemplateClass<bool, actualClass, int, actualClass2*>, type="<bool, , int, *>", typeDefs={ref(TemplateClass), ref(actualClass), ref(actualClass2)}
		Object[][] actualClasses = null;
		if (!typeDefs.isEmpty()) {
			String[] modifiers = new String[0];
			if (beginIndex != -1 && endIndex > beginIndex) {
				String typeModifierString = templateString.substring(beginIndex + 1, endIndex);
				modifiers = typeModifierString.split(",");
			}
			int index = (templateString.indexOf("<") == 0) ? 1 : 0;
			actualClasses = new Object[modifiers.length][];
			for (int i = 0, j = index; i < modifiers.length; i++) {
				Object[] filterKeyword = filterKeyword(modifiers[i].trim());
				if (filterKeyword[1].equals("")) {
					IClass actualCls = (IClass) CompoundDef.compounddef.get(typeDefs.get(j++).getRefid());
					actualClasses[i] = new Object[] {actualCls, filterKeyword[0]};
				} else {
					actualClasses[i] = new Object[] {filterKeyword[1], filterKeyword[0]};
				}
			}
		} else {
			String[] params = templateString.substring(beginIndex + 1, endIndex).split(",");
//			if (nameSpace != null) {
//				actualClasses = getActualClassList(nameSpace, params, typeDefs);
//			} else {
//				actualClasses = getActualClassList(parent, params, typeDefs);
//			}
			actualClasses = new Object[params.length][];
			for (int i = 0; i < params.length; i++) {
				Object[] filterKeyword = filterKeyword(params[i].trim());
				actualClasses[i] = new Object[] {filterKeyword[1], filterKeyword[0]};
			}
		}
		
		ITemplateBinding[] tBindings = templateClass.getTemplateBindings();
		templateString = filterAnonimousString(templateString, typeDefs);
		for (int i = 0; i < tBindings.length; i++) {
			ITemplateBinding tBinding = tBindings[i];
			if (tBinding.getTemplate() == templateClass 
					&& matchesAllActualValues(tBinding, actualClasses)) {
				IClass boundClass = tBinding.getBoundElement();
				if ("".equals(boundClass.getName())
						|| templateString.equals(boundClass.getName())) {
					return boundClass;
				}
			}
		}
		BasicModelEditor basicModelEditor = ModelEditorFactory.getBasicModelEditor();
		IClassifierTemplateParameter[] tParams = templateClass.getTemplateParameters();
		if (tParams.length < actualClasses.length) {
			IClass paraType = null;
			for (int i = 0; i < actualClasses.length - tParams.length; i++) {
				basicModelEditor.createTemplateParameter(templateClass, "param" + i, paraType, null);
			}
		}
		tParams = templateClass.getTemplateParameters();
		IClass anonimousClass = null;
		if (nameSpace != null) {
			anonimousClass = getTempClassForAnominousClass(nameSpace);
		} else {
			anonimousClass = getTempClassForAnominousClass(parent);
		}
		ITemplateBinding binding = basicModelEditor.createTemplateBinding(anonimousClass, templateClass);
		for (int i = 0; i < actualClasses.length; i++) {
			IClassifierTemplateParameter param = (IClassifierTemplateParameter)tParams[i];
			try {
				binding.addActualParameter(param, actualClasses[i][0]);
				if (!"".equals(actualClasses[i][1])) {
					binding.setActualParameterTypeModifier(param, actualClasses[i][1].toString());
				}
			} catch (InvalidEditingException e) {
				//do nothing
			}
		}
		
		anonimousClass.setName("");
		return anonimousClass;
	}
	
	public static String filterAnonimousString(String typeString, List<Ref> typeDefs) {
		int beginIndex = typeString.indexOf("<");
		int endIndex = typeString.lastIndexOf(">");
		if (beginIndex == -1 || endIndex == -1) {
			return typeString;
		}
		String head = typeString.substring(0, beginIndex).trim();
		String endChar = typeString.substring(endIndex + 1).trim();
		String[] contents = typeString.substring(beginIndex + 1, endIndex).split(",");
		String cType = "";
		head = Tool.filterInvalidChar(head);
		if ("".equals(head) && !typeDefs.isEmpty()) {
			head = typeDefs.get(0).value;
		} 
		cType = head + "<";
		for (int i = 0; i < contents.length; i++) {
			contents[i] = Tool.filterInvalidChar(contents[i]);
			contents[i] = filterAnonimousString(contents[i], typeDefs);
			if (("".equals(contents[i].trim()) 
					|| contents[i].trim().equals("*") 
					|| contents[i].trim().equals("&")
					|| contents[i].trim().equals("**")) 
					&& !typeDefs.isEmpty()) {
				contents[i] = (typeDefs.get(0).value.trim() + " " + contents[i].trim()).trim();
			}
			cType += contents[i].trim();
			if (i < contents.length - 1) {
				cType += ",";
			}
		}
		cType += ">";
		cType += " " + endChar;
		return cType.trim();
	}
	
	private static boolean matchesAllActualValues(ITemplateBinding binding, Object[] actualClasses) throws InvalidEditingException {
		List matchedParams = new ArrayList();
		Map actualMap = binding.getActualMap();
		for (int i = 0; i < actualClasses.length; i++) {
			Object[] actual = (Object[])actualClasses[i];
			for (Iterator it = actualMap.keySet().iterator(); it.hasNext(); ) {
				IClassifierTemplateParameter param = (IClassifierTemplateParameter)it.next();				
				if (actualMap.get(param).equals(actual[0])
				      && actual[1].equals(binding.getActualParameterTypeModifier(param))
				      && !matchedParams.contains(param)) {
					matchedParams.add(param);
					break;
				}
			}
		}
		if (matchedParams.size() == actualClasses.length) {
			return true;
		}
		
		return false;
	}
	
	private static Object[][] getActualClassList(Object parent, String[] names, List<Ref> typeDefs) 
	throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		if (names.length == 0 && !typeDefs.isEmpty()) {
			IClass actualClass = getClass(parent, typeDefs.get(0).getValue());
			return new Object[][]{{actualClass, ""}};
		}
		Object[][] actualAndModifier = new Object[names.length][2];
		for (int i = 0; i < names.length; i++) {
			names[i] = filterInvalidChar(names[i]);
			Object actualClass;
			
			Object[] result = filterKeyword(names[i]);
			String type = ((String) result[1]).trim();
			if ("".equals(type) && !typeDefs.isEmpty()) {
				type = typeDefs.get(0).value;
			}
			String filteredType = Member.getTypeFromTypeDef(type.trim());
			actualClass = getAnonimousClass(parent, null, filteredType, typeDefs);
			if (actualClass == null) {
				if (LanguageManager.getCurrentLanguagePrimitiveType().contains(filteredType.trim())) {
					actualClass = filteredType.trim();
				} else {
					String[] path = filteredType.trim().split("::");
					if (path.length > 1) {
						String[] namespace = new String[path.length - 1];
						System.arraycopy(path, 0, namespace, 0, path.length - 1);
						actualClass = getClass(namespace, path[path.length - 1].trim());
					} else {
						actualClass = getClass(parent, filteredType.trim());
					}
				}
			}
			actualAndModifier[i][0] = actualClass;
			actualAndModifier[i][1] = result[0];
		}
		return actualAndModifier;
	}
	
	private static Object[] filterKeyword(String type) {
		String typeModifier = "";
		if (type.trim().endsWith("&")) {
			type = type.replaceFirst("&", "").trim();
			typeModifier = "&";
		}
		if (type.trim().endsWith("**")) {
			type = type.replaceFirst("\\*" + "\\*", "").trim();
			typeModifier = "**";
		}
		
		if (type.trim().endsWith("*")) {
			type = type.replaceFirst("\\*", "").trim();
			typeModifier = "*";
		}
		return new Object[] {typeModifier, type};
	}
	
	private static IClass getClass(Object parent, String name) throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		if (parent instanceof IPackage) {
			return getClass((IPackage) parent, name.trim());
		} else if (parent instanceof IClass) {
			return getClass((IClass) parent, name.trim());
		} else if (parent instanceof String[]) {
			return getClass((String[]) parent, name.trim());
		}
		return null;
	}
	
	private static IClass getTempClassForAnominousClass(Object parent) throws ProjectNotFoundException, ClassNotFoundException, InvalidEditingException {
		String className = "className";
		int classNum = 0;
		INamedElement[] namedElements;
		if (parent instanceof IPackage) {
			namedElements = ((IPackage) parent).getOwnedElements();
			while(containsSameName(namedElements, className)) {
				className += classNum++;
			}
			return getClass((IPackage) parent, className);
		} else if (parent instanceof IClass) {
			namedElements = ((IClass) parent).getNestedClasses();
			while(containsSameName(namedElements, className)) {
				className += classNum++;
			}
			return getClass((IClass) parent, className);
		} else if (parent instanceof String[]) {
			IPackage iPkg = getPackage((String[]) parent);
			namedElements = iPkg.getOwnedElements();
			while(containsSameName(namedElements, className)) {
				className += classNum++;
			}
			return getClass((String[]) parent, className);
		}
		return null;
	}
	
	private static boolean containsSameName(INamedElement[] namedElements, String name) {
		for (int i = 0; i < namedElements.length; i++) {
			if (namedElements[i].getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public static String filterInvalidChar(String string) {
		int index = string.trim().indexOf("::");
		if (index == 0) {
			string = string.replaceFirst("::", "").trim();
		}
		return string;
	}
}