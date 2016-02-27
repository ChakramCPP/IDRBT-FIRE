# -*- coding: utf-8 -*-

""" 

Evaluation script for transliteration (of subtask-1) task in 2014 FIRE Shared Task on Transliterated Search.

Function based on evaluation script by Rishiraj Saha Roy (rishiraj.saharoy@gmail.com), Spandana and Jatin (Former interns at MSR-India). This was used in FIRE 2013 Shared Task on Transliterated Search.
New additions: Extra rules for Bangla; modifications for Kannada, Malayalam, and Tamil.

Report bugs to: gokulchittaranjan@gmail.com

"""

def check_equivalence(word1, word2):
        """
        Checks whether two Indic script words are equivalent or not. It handles these cases-
        1. homorganic nasal case
            check_equivalence('वाङ्ग्मय', 'वांग्मय')
        2. anuswar-chandraBindu exchange
            check_equivalence('इन्तेहाँ', 'इन्तेहां')
        3. non-obligatory use of Nukta
            check_equivalence('आवाज', 'आवाज़')
        4. homophonic ending
            check_equivalence('सताये', 'सताए')
        5. canonical unicodes
            check_equivalence('इज़हार', 'इज़हार') | 'ज़'=u'\u095b' and 'ज़' = u'\u091c\u093c\'
        6. popular usage [not supported]
            check_equivalence('तनहाई', 'तन्हाई')

        Category-1: homorganic nasal case
        check_equivalence('वाङ्ग्मय', 'वांग्मय') TRUE
        check_equivalence('व्यञ्जन','व्यंजन') TRUE
        check_equivalence('पाखण्ड', 'पाखंड') TRUE
        check_equivalence('अन्त', 'अंत') TRUE
        check_equivalence('चम्पा', 'चंपा') TRUE

        Category-2: anuswar-chandraBindu exchange
        check_equivalence('इन्तेहाँ', 'इन्तेहां') TRUE
        check_equivalence('हंस', 'हँस') TRUE

        Category-3: non-obligatory use of Nukta
        check_equivalence('आवाज', 'आवाज़') TRUE

        Category-4: homophonic ending
        check_equivalence('सताये', 'सताए') TRUE
        check_equivalence('सताये', 'सताऐ') FALSE
        check_equivalence('येतबार', 'एतबार') FALSE
        check_equivalence('ये', 'ए') FALSE
        check_equivalence('हवाएं', 'हवायें') TRUE

        Category-5: canonical unicodes
        precomposed = u"इज़हार"; #u'\u0907\u095b\u0939\u093e\u0930'
        decomposed = u"इज़हार"; #u'\u0907\u091c\u093c\u0939\u093e\u0930'
        check_equivalence('इज़हार', 'इज़हार')

        Category-6: popular usage
        check_equivalence('तनहाई', 'तन्हाई')
        check_equivalence('ख़्याल', 'ख़याल')

        """
        import unicodedata as ud
	word1x = ud.normalize('NFD', word1.decode('utf8'))
	word2x = ud.normalize('NFD', word2.decode('utf8'))

        sign_nukta = ['DEVANAGARI SIGN NUKTA','GUJARATI SIGN NUKTA','BENGALI SIGN NUKTA', 'KANNADA SIGN NUKTA']
        sign_virama = ['DEVANAGARI SIGN VIRAMA','GUJARATI SIGN VIRAMA','BENGALI SIGN VIRAMA', 'TAMIL SIGN VIRAMA', 'MALAYALAM SIGN VIRAMA']
        sign_anusvara = ['DEVANAGARI SIGN ANUSVARA','GUJARATI SIGN ANUSVARA','BENGALI SIGN ANUSVARA', 'KANNADA SIGN ANUSVARA', 'TAMIL SIGN ANUSVARA', 'MALAYALAM SIGN ANUSVARA']
        sign_candrabindu = ['DEVANAGARI SIGN CANDRABINDU','GUJARATI SIGN CANDRABINDU','BENGALI SIGN CANDRABINDU']


        vowel_e = ['DEVANAGARI VOWEL SIGN E','GUJARATI VOWEL SIGN E','BENGALI VOWEL SIGN E','KANNADA VOWEL SIGN E','TAMIL VOWEL SIGN E','MALAYALAM VOWEL SIGN E']
        letter_na = ['DEVANAGARI LETTER NA','GUJARATI LETTER NA','BENGALI LETTER NA','KANNADA LETTER NA', 'TAMIL LETTER NA', 'MALAYALAM LETTER NA']
        letter_nga = ['DEVANAGARI LETTER NGA','GUJARATI LETTER NGA','BENGALI LETTER NGA', 'KANNADA LETTER NGA', 'TAMIL LETTER NGA', 'MALAYALAM LETTER NGA']
        letter_nya = ['DEVANAGARI LETTER NYA','GUJARATI LETTER NYA','BENGALI LETTER NYA', 'KANNADA LETTER NYA', 'TAMIL LETTER NYA', 'MALAYALAM LETTER NYA']
        letter_nna = ['DEVANAGARI LETTER NNA','GUJARATI LETTER NNA','BENGALI LETTER NNA', 'KANNADA LETTER NNA', 'TAMIL LETTER NNA', 'MALAYALAM LETTER NNA']
        letter_ma = ['DEVANAGARI LETTER MA','GUJARATI LETTER MA','BENGALI LETTER MA', 'KANNADA LETTER MA', 'TAMIL LETTER MA', 'MALAYALAM LETTER MA']
        letter_e = ['DEVANAGARI LETTER E','GUJARATI LETTER E','BENGALI LETTER E', 'KANNADA LETTER E', 'TAMIL LETTER E', 'MALAYALAM LETTER E']
        letter_ya = ['DEVANAGARI LETTER YA','GUJARATI LETTER YA','BENGALI LETTER YA', 'KANNADA LETTER YA', 'TAMIL LETTER YA', 'MALAYALAM LETTER YA']


        bengali_e_fix = ['BENGALI VOWEL SIGN I', 'BENGALI VOWEL SIGN II']
        bengali_u_fix = ['BENGALI VOWEL SIGN U', 'BENGALI VOWEL SIGN UU'];

        bengali_letter_e_fix = ['BENGALI LETTER I', 'BENGALI LETTER II'];
        bengali_letter_u_fix = ['BENGALI LETTER U', 'BENGALI LETTER UU'];

	names1 = map(ud.name, word1x)
	names2 = map(ud.name, word2x)
	if len(names1) > len(names2):
		temp = names2
		names2 = names1
		names1 = temp
	i=0
	j=0
	#print names1
	#print names2
	while i <len(names1) and j-1<len(names2):
		#print i, j , len(names1), len(names2)
		if names1[i] == names2[j] or (names1[i] in sign_anusvara and names2[j] in sign_candrabindu) or (names1[i] in sign_candrabindu and names2[j] in sign_anusvara) or (names1[i] in bengali_e_fix and names2[i] in bengali_e_fix) or (names1[i] in bengali_u_fix and names2[i] in bengali_u_fix) or (names1[i] in bengali_letter_e_fix and names2[i] in bengali_letter_e_fix) or (names1[i] in bengali_letter_u_fix and names2[i] in bengali_letter_u_fix):
			if j+1<len(names2) and (names2[j+1] in sign_nukta and ((i+1 < len(names1) and names1[i+1] not in sign_nukta) or (i+1 >= len(names1)))):
				j+=1
			i+=1
			j+=1
		elif (names1[i] in sign_anusvara and (names2[j] in letter_na or names2[j] in letter_nga or names2[j] in letter_nya or names2[j] in letter_nna or names2[j] in letter_ma) and j+1< len(names2) and names2[j+1] in sign_virama):
			i+=1
			j+=2
                elif (names1[i] in letter_e and (i+1 >= len(names1) or names1[i+1] in sign_anusvara) and i>0 and (names2[j] in letter_ya and j+1 < len(names2) and names2[j+1] in vowel_e)):
			i+=1
			j+=2
		else:
		     return False

		if i == len(names1) and j== len(names2):
		    return True


if __name__=="__main__":
        
        print "check_equivalence('वाङ्ग्मय', 'वांग्मय') TRUE"
        print check_equivalence('वाङ्ग्मय', 'वांग्मय')
        print "check_equivalence('व्यञ्जन','व्यंजन') TRUE"
        print check_equivalence('व्यञ्जन','व्यंजन')
        print "check_equivalence('पाखण्ड', 'पाखंड') TRUE"
        print check_equivalence('पाखण्ड', 'पाखंड')
        print "check_equivalence('अन्त', 'अंत') TRUE"
        print check_equivalence('अन्त', 'अंत')
        print "check_equivalence('चम्पा', 'चंपा') TRUE"
        print check_equivalence('चम्पा', 'चंपा')

        print "Category-2: anuswar-chandraBindu exchange"
        print "check_equivalence('इन्तेहाँ', 'इन्तेहां') TRUE"
        print check_equivalence('इन्तेहाँ', 'इन्तेहां')
        print "check_equivalence('हंस', 'हँस') TRUE"
        print check_equivalence('हंस', 'हँस')

        print "Category-3: non-obligatory use of Nukta"
        print "check_equivalence('आवाज', 'आवाज़') TRUE"
        print check_equivalence('आवाज', 'आवाज़');

        print "Category-4: homophonic ending"
        print "check_equivalence('सताये', 'सताए') TRUE"
        print check_equivalence('सताये', 'सताए');
        print "check_equivalence('सताये', 'सताऐ') FALSE"
        print check_equivalence('सताये', 'सताऐ')
        print "check_equivalence('येतबार', 'एतबार') FALSE"
        print check_equivalence('येतबार', 'एतबार');
        print "check_equivalence('ये', 'ए') FALSE"
        print check_equivalence('ये', 'ए')
        print "check_equivalence('हवाएं', 'हवायें') TRUE"
        print check_equivalence('हवाएं', 'हवायें')

        print "Category-5: canonical unicodes"
        precomposed = u"इज़हार"; #u'\u0907\u095b\u0939\u093e\u0930'
        decomposed = u"इज़हार"; #u'\u0907\u091c\u093c\u0939\u093e\u0930'
        print "check_equivalence('इज़हार', 'इज़हार')"
        print check_equivalence('इज़हार', 'इज़हार')

        print "Category-6: popular usage"
        
        print "check_equivalence('तनहाई', 'तन्हाई')"
        print check_equivalence('तनहाई', 'तन्हाई')
        print "check_equivalence('ख़्याल', 'ख़याल')"
        print check_equivalence('ख़्याल', 'ख़याल')
