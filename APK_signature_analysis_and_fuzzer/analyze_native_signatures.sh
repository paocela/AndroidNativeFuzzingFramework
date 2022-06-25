#!/bin/bash

## WHEN NOT RUN OVER ADB (for example over termux)

## output colors ##
RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m'

# help option
Help()
{
   # Display Help
   echo "Syntax: ./analyze_native_signatures [-h] [qdox|grep]"
   echo
   echo "Analyze APK native methods signatures, group by and count them"
   echo
   echo "Requirements:"
   echo "   - target_APK folder structure:"
   echo "      ├── target_APK/"
   echo "      │   ├── App-Name/"
   echo "      │   │	  └── base.apk"
   echo "      │   └── ..."
   echo
   echo "Options:"
   echo "   -h, --help     Print this Help."
   echo "   [qdox|grep]    Choose static analysis method (only qdox implemented, it's more precise)"
   echo
}


Analyze()
{
	# cleanup
	rm signatures_all.txt

	if [ "$1" == "qdox" ]; then
		echo -e "${GREEN}[LOG]${NC} Using QDOX (high workload)"
		QDOX=true # use qdox	
	else
		echo -e "${GREEN}[LOG]${NC} Using grep (low precision)"
		QDOX=false # use grep
	fi

	TOTAL_NUM_APK=$(ls target_APK/ | wc -l)
	CURRENT_NUM_APK=1
		
	# loop for all apps
	for TARGET_DIR in ./target_APK/*/; do
		APP_NAME=$(echo $TARGET_DIR | cut -d "/" -f 3)

		echo -e "${GREEN}[LOG]${NC} Analyzing $APP_NAME ($CURRENT_NUM_APK/$TOTAL_NUM_APK)"

		## extract /lib folder (for harness usage later on) ##

		cp "$TARGET_DIR/base.apk" "$TARGET_DIR/base.zip"
		unzip -q "$TARGET_DIR/base.zip" -d "$TARGET_DIR/base_apk"
		if [ -d "$TARGET_DIR/base_apk/lib/arm64-v8a" ] ; then
			mv "$TARGET_DIR/base_apk/lib/" "$TARGET_DIR"
		elif [ -d "$TARGET_DIR/base_apk/lib/arm64" ] ; then
			mv "$TARGET_DIR/base_apk/lib/arm64" "$TARGET_DIR/base_apk/lib/arm64-v8a"
			mv "$TARGET_DIR/base_apk/lib/" "$TARGET_DIR"
		else
			echo -e "${RED}[ERR]${NC} App $APP_NAME either not native or APK doesn't provide arm64 version of libraries"
		fi
		rm "$TARGET_DIR/base.zip"
		rm -rf "$TARGET_DIR/base_apk"

		## decompile apk ##
		
		echo -e "${GREEN}[LOG]${NC} Decompiling $APP_NAME"
		./jadx/build/jadx/bin/jadx -d "$TARGET_DIR/base" -r "$TARGET_DIR/base.apk"

		## exctract native methods ##

		echo -e "${GREEN}[LOG]${NC} Extracting native methods from $APP_NAME"

		# using qdox
		if [ "$QDOX" = true ] ; then
			cd extractor_pattern

			# loop until exctracted correctly
			FLAG=true
			while [ "$FLAG" = true ] ; do
				# extract using qdox
				EXTRACT_OUT=$(java -cp 'com.qdox.jar:.' extractor $APP_NAME 2>&1)

				if [ $? -eq 0 ] ; then
					# if successful, exit
					echo -e "$EXTRACT_OUT"
					FLAG=false
				else
					# if present file qdox can't parse, remove and keep trying
					# get buggy filename
					EXTRACT_OUT=$(awk 'NR > 2 { print }' <<< "$EXTRACT_OUT")
					ERROR_FILE=$(awk '{ sub(/.*file:/, ""); sub(/* at */, ""); print $1}' <<< "$EXTRACT_OUT" | head -n 1)
					# safety check (prevent any unpleasent behaviour)
					if [[ $ERROR_FILE == *"/APK_signature_analysis_and_fuzzer/extractor_pattern/../target_APK/"*  ]]; then
						echo -e "${YELLOW}[WRN]${NC} Removing buggy file $ERROR_FILE"
						# remove it
						rm $ERROR_FILE
					fi
				fi
			done
			cd ..

			# add new signatures to all others (not function names)
			cut -d " " -f 2 "$TARGET_DIR/signatures_pattern.txt" >> "signatures_all.txt"
		fi

		echo -e "${GREEN}[LOG]${NC} Done for $APP_NAME"
		echo ""

		((CURRENT_NUM_APK=CURRENT_NUM_APK+1))
	done

	if [ "$QDOX" = false ]; then
		# using grep
		cd target_APK

		# NOT FINISHED (too complex and not precise)
		grep -r -E -h --color "public native|public static native|public native final|public final native" |\
		 	sed "s/public native finale//g" |\
 	        sed "s/public final native//g" |\
	 	    sed "s/public static native//g" |\
	 	    sed "s/public native//g" |\
	 	    sed 's/^ *//g' | tr "(" " " | cut -d" " -f 1,3- | tr ");" " "
	 	    

	 	grep -r -E -h --color "public native|public static native|public native final|public final native" | sed "s/public native finale//g" | sed "s/public final native//g" | sed "s/public static native//g" | sed "s/public native//g" | sed 's/^ *//g' | tr "(" " " | sed '/*/!s/^\([^ ]*\) \([^ ]*\)/\2 \1/' | sed 's/ /:/2' | sed 's/ /-/1' | sed 's/ .*,/ /' | sed 's/ /_/1' | sed 's/ .*);/ /'
	 	    
		
		cd ..
	fi

	echo "${GREEN}[LOG]${NC} Counting signature occurrences (in signatures_all.txt)"
	sort signatures_all.txt | uniq -c | sort -g
}

# Main
if [ "$#" -eq 0 ]; then
    echo "Try './analyze_native_signatures.sh --help' for more information."
    exit 1
elif [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    Help
    exit 0
elif [[ "$#" -ne 1 || ("$1" != "qdox"  &&  "$1" != "grep") ]]; then
    echo "Error usage..."
    Help
    exit 1
else
    Analyze $1
    exit 0
fi
