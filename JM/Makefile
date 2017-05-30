###
###     Makefile for JM H.264/AVC encoder/decoder
###
###             generated for UNIX/LINUX environments
###             by Limin Wang(lance.lmwang@gmail.com) 
###

SUBDIRS := ldecod

### include debug information: 1=yes, 0=no
DBG?= 0
### enforce 32-bit build : 1=yes, 0=no
M32?= 0
### static link : 1=yes, 0=no
STC?= 0
### OPENMP support : 1=yes, 0=no
OPENMP?= 0

#check for LLVM and silence warnings accordingly
LLVM = $(shell $(CC) --version | grep LLVM)
ifneq ($(LLVM),)
	CFLAGS+=-Qunused-arguments
else
	CFLAGS+=-Wno-unused-but-set-variable
endif

export DBG
export STC 
export M32
export OPENMP
export CFLAGS

.PHONY: default all distclean clean tags depend $(SUBDIRS)

default: all

all: $(SUBDIRS)

$(SUBDIRS):
	$(MAKE) -C $@

clean depend:
	@echo "Cleaning dependencies"
	@for i in $(SUBDIRS); do make -C $$i $@; done

tags:
	@echo "update tag table at top directory"
	@ctags -R .
	@for i in $(SUBDIRS); do make -C $$i $@; done

distclean: clean
	@echo "Cleaning all"
	@rm -f tags
	@for i in $(SUBDIRS); do make -C $$i $@; done

