import re;

def readAnnotationFile(filename):
    nere = re.compile(r"\[[^\]\\]+\]");
    fid = open(filename,'r');
    data = [];
    isTokType = re.compile("^[A-Z]{1,3}$");
    neTokTypes = set(["P","L","A"]);
    
    for line in fid:
        line = line.replace("\n","").replace("\r","").decode('utf-8').replace(u"\ufeff","").strip();
        #print [line]
        if line=="":
            continue;
        neLocs = [];
        it = re.finditer(nere, line);
        for match in it:
            neLocs.append([match.start(), match.end()]);
        fields = line.split(" ");
        st = 0;
        entry = [];
        for f in fields:
            inNe = False;
            rloc = [];
            for loc in neLocs:
                if st>=loc[0] and st<=loc[1]:
                    rloc = loc;
                    inNe = True;
                    break;
            tok = "";
            tokType = "";
            translit = "";
            if inNe:
                tok = f;
                if rloc[0]==st and rloc[1]<=st+len(f):
                    tok = line[st+1:rloc[1]-1];
                #if rloc[0]==st:
                #    tok = tok[1:];
                #if rloc[1]==st+len(f)-1:
                #    tok = tok[:-2];
                postfix = line[rloc[1]:];
                if len(postfix)>0:
                    temp = postfix.split(" "); # translit of multi-token ne not handled here...
                    if len(temp)>0:
                        if "=" in temp[0]:
                            tokTypes = temp[0].split("=")[0];
                            if (tokTypes==""):
                                tokType = "NE_?";
                            else:
                                tokType = "NE_%s" %(tokTypes);
                            translit = temp[0].split("=")[1];
                        else:
                            if temp[0]=="":
                                tokType = "NE_?";
                            else:
                                tokType = "NE_%s" %(temp[0]);
                    else:
                        if "=" in postfix:
                            tokTypes = postfix.split("=")[0];
                            if tokTypes=="":
                                tokType = "NE_?";
                            else:
                                tokType = "NE_%s" %(tokTypes);
                            translit = postfix.split("=")[1];
                        else:
                            if postfix=="":
                                tokType = "NE_?";
                            else:
                                tokType = "NE_%s" %(postfix);
                """
                if len(postfix)==0:
                    tokType = "NE_GENERIC";
                elif postfix[0]==" ":
                    tokType = "NE_GENERIC";
                elif not postfix[0] in ["P","O","L","A"]:
                    tokType = "NE_?";
                else:
                    tokType = "NE_%s" %(postfix[0]);
                """

            else:
                if "\\" in f:
                    temp = f.split("\\");
                    tok = temp[0];
                    if "=" in temp[1]:
                        translit = temp[1].split("=")[1];
                        tokType = temp[1].split("=")[0];
                    else:
                        tokType = temp[1];

                    if re.match(isTokType, tokType)==None:
                        tokType = "";
                        tok = f;
                        translit = "";
                else:
                    tok = f;
            st += len(f)+1;
            
            # Fixes to convert NE naming inconsistencies
            if tokType[0:3]!="NE_":
                if len(set(tokType).intersection(neTokTypes))>0:
                    tokType = "NE_%s" %(tokType);
                if tokType=="N":
                    tokType = "NE_?";
            if tok=="":
                continue;
            entry.append([tok, tokType, translit]);
        data.append(entry);
    return data;
