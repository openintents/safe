set PATH=.;c:\cygwin\bin;%PATH%
bash androidxml2po.bash -e
mkdir translations_safe
copy safe* translations_safe
tar -cvvzf translations_safe.tgz translations_safe